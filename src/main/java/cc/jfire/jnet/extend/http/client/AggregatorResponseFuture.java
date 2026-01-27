package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.UNSAFE;
import cc.jfire.jnet.extend.http.dto.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.zip.GZIPInputStream;

/**
 * 聚合模式的 ResponseFuture 实现。
 * <p>
 * 状态机说明：
 * - onReceive 和 onFail 由 IO 线程调用，且互斥（error 后通道关闭，不会再有 onReceive）
 * - waitForEnd 由业务线程调用
 * - 超时由业务线程设置，IO 线程在收到数据时检查超时状态
 */
public class AggregatorResponseFuture implements ResponseFuture
{
    // 状态常量：bit0=结束, bit1=超时, bit2=错误
    private static final int                  NO_ERROR_NO_TIMEOUT_NO_END = 0b000; // 初始状态
    private static final int                  NO_ERROR_NO_TIMEOUT_END    = 0b001; // 正常完成
    private static final int                  NO_ERROR_TIMEOUT_NO_END    = 0b010; // 超时但未结束
    private static final int                  NO_ERROR_TIMEOUT_END       = 0b011; // 超时后结束
    private static final int                  ERROR_NO_TIMEOUT_NO_END    = 0b100; // 错误未超时未结束
    private static final int                  ERROR_TIMEOUT_NO_END       = 0b110; // 错误且超时未结束
    private static final long                 STATUS_OFFSET              = UNSAFE.getFieldOffset("status", AggregatorResponseFuture.class);
    private final        BufferAllocator      allocator;
    private volatile     int                  status                     = NO_ERROR_NO_TIMEOUT_NO_END;
    private volatile     Throwable            error;
    private volatile     HttpResponse         httpResponse;
    private volatile     Thread               waitingThread;
    private              HttpResponsePartHead headPart;
    private              IoBuffer             bodyBuffer;

    public AggregatorResponseFuture(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    private boolean isTimeout(int status)
    {
        return (status & 0b010) != 0;
    }

    private boolean isError(int status)
    {
        return (status & 0b100) != 0;
    }

    @Override
    public void onReceive(HttpResponsePart part)
    {
        if (part instanceof HttpResponsePartHead head)
        {
            if (isTimeout(status))
            {
                head.free();
                return;
            }
            this.headPart = head;
            head.free();
            long contentLength = head.getContentLength();
            if (contentLength > Integer.MAX_VALUE)
            {
                part.free();
                throw new IllegalArgumentException("响应头 Content-Length 超过 Integer.MAX_VALUE");
            }
            if (contentLength > 0)
            {
                bodyBuffer = allocator.allocate((int) contentLength);
            }
            else if (head.isChunked())
            {
                bodyBuffer = allocator.allocate(1024);
            }
            if (head.isLast())
            {
                handleEnd();
            }
        }
        else if (part instanceof HttpResponseFixLengthBodyPart fixPart)
        {
            if (isTimeout(status))
            {
                fixPart.free();
                return;
            }
            appendFixLength(fixPart);
            if (fixPart.isLast())
            {
                handleEnd();
            }
        }
        else if (part instanceof HttpResponseChunkedBodyPart chunkedPart)
        {
            if (isTimeout(status))
            {
                chunkedPart.free();
                return;
            }
            appendChunked(chunkedPart);
            if (chunkedPart.isLast())
            {
                handleEnd();
            }
        }
    }

    private void handleEnd()
    {
        while (true)
        {
            int currentStatus = status;
            if (currentStatus == NO_ERROR_NO_TIMEOUT_NO_END)
            {
                HttpResponse response = assembleResponse();
                if (UNSAFE.compareAndSwapInt(this, STATUS_OFFSET, NO_ERROR_NO_TIMEOUT_NO_END, NO_ERROR_NO_TIMEOUT_END))
                {
                    this.httpResponse = response;
                    wakeUpWaitingThread();
                    break;
                }
                response.free();
            }
            else if (currentStatus == NO_ERROR_TIMEOUT_NO_END)
            {
                if (UNSAFE.compareAndSwapInt(this, STATUS_OFFSET, NO_ERROR_TIMEOUT_NO_END, NO_ERROR_TIMEOUT_END))
                {
                    releaseBodyBuffer();
                    break;
                }
            }
            else
            {
                break;
            }
        }
    }

    private void appendFixLength(HttpResponseFixLengthBodyPart fixPart)
    {
        try
        {
            IoBuffer partBuffer = fixPart.getPart();
            if (partBuffer == null)
            {
                return;
            }
            if (bodyBuffer == null)
            {
                bodyBuffer = allocator.allocate(partBuffer.remainRead());
            }
            bodyBuffer.put(partBuffer);
        }
        finally
        {
            fixPart.free();
        }
    }

    private void appendChunked(HttpResponseChunkedBodyPart chunkedPart)
    {
        try
        {
            IoBuffer partBuffer = chunkedPart.getPart();
            if (partBuffer == null)
            {
                return;
            }
            int headLength      = chunkedPart.getHeadLength();
            int chunkLength     = chunkedPart.getChunkLength();
            int effectiveLength = chunkLength - headLength - 2;
            if (effectiveLength > 0)
            {
                int oldReadPosi = partBuffer.getReadPosi();
                partBuffer.addReadPosi(headLength);
                if (bodyBuffer == null)
                {
                    bodyBuffer = allocator.allocate(effectiveLength);
                }
                bodyBuffer.put(partBuffer, effectiveLength);
                partBuffer.setReadPosi(oldReadPosi);
            }
        }
        finally
        {
            chunkedPart.free();
        }
    }

    private HttpResponse assembleResponse()
    {
        HttpResponse response = new HttpResponse();
        if (headPart != null)
        {
            response.getHead().setVersion(headPart.getVersion());
            response.getHead().setStatusCode(headPart.getStatusCode());
            response.getHead().setReasonPhrase(headPart.getReasonPhrase());
            response.getHead().setHeaders(headPart.getHeaders());
        }
        if (bodyBuffer != null && headPart != null)
        {
            String contentEncoding = headPart.getHeaders().get("Content-Encoding");
            if ("gzip".equalsIgnoreCase(contentEncoding))
            {
                bodyBuffer = decompressGzip(bodyBuffer);
            }
        }
        response.setBodyBuffer(bodyBuffer);
        bodyBuffer = null;
        return response;
    }

    private IoBuffer decompressGzip(IoBuffer compressedBuffer)
    {
        int compressedSize = compressedBuffer.remainRead();
        if (compressedSize == 0)
        {
            return compressedBuffer;
        }
        byte[] compressedBytes = new byte[compressedSize];
        compressedBuffer.get(compressedBytes);
        compressedBuffer.free();
        try (ByteArrayInputStream bais = new ByteArrayInputStream(compressedBytes);
             GZIPInputStream gzis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream())
        {
            byte[] buffer = new byte[4096];
            int    len;
            while ((len = gzis.read(buffer)) != -1)
            {
                baos.write(buffer, 0, len);
            }
            byte[]   decompressedBytes  = baos.toByteArray();
            IoBuffer decompressedBuffer = allocator.allocate(decompressedBytes.length);
            decompressedBuffer.put(decompressedBytes);
            return decompressedBuffer;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Failed to decompress gzip body", e);
        }
    }

    @Override
    public void onFail(Throwable error)
    {
        this.error = error;
        while (true)
        {
            int currentStatus = status;
            if (currentStatus == NO_ERROR_NO_TIMEOUT_NO_END)
            {
                if (UNSAFE.compareAndSwapInt(this, STATUS_OFFSET, NO_ERROR_NO_TIMEOUT_NO_END, ERROR_NO_TIMEOUT_NO_END))
                {
                    releaseBodyBuffer();
                    wakeUpWaitingThread();
                    break;
                }
            }
            else if (currentStatus == NO_ERROR_TIMEOUT_NO_END)
            {
                if (UNSAFE.compareAndSwapInt(this, STATUS_OFFSET, NO_ERROR_TIMEOUT_NO_END, ERROR_TIMEOUT_NO_END))
                {
                    releaseBodyBuffer();
                    break;
                }
            }
            else
            {
                break;
            }
        }
    }

    private void releaseBodyBuffer()
    {
        if (bodyBuffer != null)
        {
            bodyBuffer.free();
            bodyBuffer = null;
        }
    }

    private void wakeUpWaitingThread()
    {
        Thread thread = waitingThread;
        if (thread != null)
        {
            waitingThread = null;
            LockSupport.unpark(thread);
        }
    }

    public HttpResponse waitForEnd(long timeoutSeconds) throws Exception
    {
        waitingThread = Thread.currentThread();
        long timeoutNanos = TimeUnit.SECONDS.toNanos(timeoutSeconds);
        long startTime    = System.nanoTime();
        while (true)
        {
            int currentStatus = status;
            if (currentStatus == NO_ERROR_NO_TIMEOUT_END)
            {
                waitingThread = null;
                return httpResponse;
            }
            if (isError(currentStatus))
            {
                waitingThread = null;
                Throwable err = error;
                if (err instanceof Exception e)
                {
                    throw e;
                }
                if (err instanceof Error e)
                {
                    throw e;
                }
                throw new RuntimeException(err);
            }
            long elapsed   = System.nanoTime() - startTime;
            long remaining = timeoutNanos - elapsed;
            if (remaining <= 0)
            {
                if (UNSAFE.compareAndSwapInt(this, STATUS_OFFSET, NO_ERROR_NO_TIMEOUT_NO_END, NO_ERROR_TIMEOUT_NO_END))
                {
                    waitingThread = null;
                    throw new SocketTimeoutException("http response wait timeout");
                }
                continue;
            }
            LockSupport.parkNanos(remaining);
            if (Thread.interrupted())
            {
                waitingThread = null;
                throw new ClosedChannelException();
            }
        }
    }
}
