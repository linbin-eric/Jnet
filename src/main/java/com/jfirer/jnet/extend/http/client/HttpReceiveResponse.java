package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 非线程并发安全的类。使用消息体的方法均只能由一个线程来使用。
 */
@Data
public class HttpReceiveResponse implements AutoCloseable
{
    /**
     * 响应完成回调接口
     */
    public interface CompletionCallback
    {
        void onCompleted(HttpReceiveResponse response);
    }

    private static final int                 RECEIVE_UN_FINISH_AND_NOT_CLOSE = 0b00;
    private static final int                 RECEIVE_UN_FINISH_AND_CLOSE     = 0b01;
    private static final int                 RECEIVE_FINISH_AND_NOT_CLOSE    = 0b10;
    private static final int                 RECEIVE_FINISH_AND_CLOSE        = 0b11;
    private static final long                STATE_OFFSET                    = UNSAFE.getFieldOffset("state", HttpReceiveResponse.class);
    private final        Pipeline            pipeline;
    private              int                 httpCode;
    private              Map<String, String> headers                         = new HashMap<>();
    /**
     * 值的取值范围有：
     * -1：代表这个响应的消息体是个不定长的以Transfer-Encoding:chunked 编码的消息体。
     * 0：代表没有响应体。
     * 正数：代表响应的消息体的字节长度。
     */
    private              int                 contentLength;
    private              String              contentType;
    private              BlockingQueue<Part> body                            = new LinkedBlockingQueue<>();
    @Getter(AccessLevel.NONE)
    private              String              decodedUTF8Body;
    private volatile     boolean             generatedUTF8Body               = false;
    private volatile     Thread              waitForReceiveFinshThread;
    private volatile     int                 state                           = RECEIVE_UN_FINISH_AND_NOT_CLOSE;
    private volatile     CompletionCallback  completionCallback;

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public void setCompletionCallback(CompletionCallback callback)
    {
        this.completionCallback = callback;
    }

    public void addPartOfBody(Part part)
    {
        body.add(part);
    }

    public void endOfBody()
    {
        body.add(Part.END);
        while (true)
        {
            switch (state)
            {
                case RECEIVE_UN_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_NOT_CLOSE, RECEIVE_FINISH_AND_NOT_CLOSE))
                    {
                        Thread thread = waitForReceiveFinshThread;
                        if (thread != null)
                        {
                            waitForReceiveFinshThread = null;
                            LockSupport.unpark(thread);
                        }
                        // 触发完成回调
                        triggerCompletionCallback();
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case RECEIVE_UN_FINISH_AND_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_CLOSE, RECEIVE_FINISH_AND_CLOSE))
                    {
                        Part part;
                        while ((part = body.poll()) != null)
                        {
                            part.free();
                        }
                        // 触发完成回调
                        triggerCompletionCallback();
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                default -> throw new IllegalStateException("endOfBody 方法只能由解码器调用，在调用这个方法的时候，消息体流的状态应该是为未解析完成");
            }
        }
    }

    private void triggerCompletionCallback()
    {
        CompletionCallback callback = this.completionCallback;
        if (callback != null)
        {
            try
            {
                callback.onCompleted(this);
            }
            catch (Exception e)
            {
                // 记录异常但不影响正常流程
                System.err.println("完成回调执行异常: " + e.getMessage());
            }
        }
    }

    /**
     * 客户端代码消费完毕响应后关闭该响应。该方法应该只能由客户端代码来调用。
     */
    @Override
    public void close()
    {
        while (true)
        {
            switch (state)
            {
                case RECEIVE_UN_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_NOT_CLOSE, RECEIVE_UN_FINISH_AND_CLOSE))
                    {
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case RECEIVE_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_FINISH_AND_NOT_CLOSE, RECEIVE_FINISH_AND_CLOSE))
                    {
                        Part part;
                        while ((part = body.poll()) != null)
                        {
                            part.free();
                        }
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                default -> throw new IllegalStateException("close 方法只能由客户端消费响应后调用，因此状态应该为 not_close 状态");
            }
        }
    }

    /**
     * 超时等待消息体被接收完整。
     * true：消息接收结束
     */
    public boolean waitForReceiveFinish(long msOfReadTimeout)
    {
        if (state == RECEIVE_UN_FINISH_AND_NOT_CLOSE)
        {
            long t0 = System.currentTimeMillis();
            waitForReceiveFinshThread = Thread.currentThread();
            while (state == RECEIVE_UN_FINISH_AND_NOT_CLOSE)
            {
                long elapsed = System.currentTimeMillis() - t0;
                long left    = msOfReadTimeout - elapsed;
                if (left > 0)
                {
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(left));
                }
                else
                {
                    return false;
                }
            }
            return true;
        }
        else if (state == RECEIVE_FINISH_AND_NOT_CLOSE)
        {
            return true;
        }
        else
        {
            throw new IllegalStateException("作为消费线程，不应该在关闭响应后仍然调用该方法");
        }
    }

    public String getCachedUTF8Body(long msOfReadTimeout) throws SocketTimeoutException
    {
        if (!generatedUTF8Body)
        {
            if (waitForReceiveFinish(msOfReadTimeout))
            {
            }
            else
            {
                throw new SocketTimeoutException("读取消息体超时，超时时间为:" + msOfReadTimeout + "毫秒");
            }
            generatedUTF8Body = true;
            IoBuffer ioBuffer = pipeline.allocator().allocate(512);
            Part     part;
            while ((part = body.poll()) != null && !part.endOfBody())
            {
                part.readEffectiveContent(ioBuffer);
                part.free();
            }
            decodedUTF8Body = StandardCharsets.UTF_8.decode(ioBuffer.readableByteBuffer()).toString();
            ioBuffer.free();
        }
        return decodedUTF8Body;
    }

    /**
     * 基于超时时间进行的消息体数据提取。
     *
     * @return
     * @throws InterruptedException
     */
    public Part pollChunk(long msOfTimeout) throws InterruptedException
    {
        return body.poll(msOfTimeout, TimeUnit.MILLISECONDS);
    }
}
