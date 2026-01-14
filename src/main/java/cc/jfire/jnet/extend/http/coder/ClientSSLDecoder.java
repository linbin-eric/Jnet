package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import lombok.Data;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Data
public class ClientSSLDecoder extends AbstractDecoder
{
    private final    SSLEngine       sslEngine;
    private          boolean         handshakeFinished = false;
    private volatile boolean         closeInbound      = false;
    private          String          remote;
    private final    CountDownLatch  handshakeLatch    = new CountDownLatch(1);

    public boolean waitHandshake(long timeout, TimeUnit unit) throws InterruptedException
    {
        return handshakeLatch.await(timeout, unit);
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (handshakeFinished)
        {
            handleData(next);
        }
        else
        {
            handshake(next, null);
        }
    }

    public synchronized void gracefulClose(Pipeline pipeline)
    {
        if (closeInbound)
        {
            return;
        }
        closeInbound = true;
        try
        {
            sslEngine.closeInbound();
        }
        catch (SSLException e)
        {
            // ignore
        }
        sslEngine.closeOutbound();
        pipeline.fireWrite(new ClientSSLProtocol().setCloseNotify(true));
        pipeline.shutdownInput();
    }

    private void handshake(ReadProcessorNode next, SSLEngineResult.HandshakeStatus hs)
    {
        while (true)
        {
            if (hs == null)
            {
                hs = sslEngine.getHandshakeStatus();
            }
            if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP || hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN)
            {
                if (accumulation == null || accumulation.remainRead() == 0)
                {
                    return;
                }
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                    accumulation.addReadPosi(result.bytesConsumed());
                    dst.free();
                    dst = null;
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        accumulation.compact();
                        if (accumulation.remainWrite() < 1000)
                        {
                            accumulation.capacityReadyFor(accumulation.capacity() * 2);
                        }
                        return;
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
                        gracefulClose(next.pipeline());
                        return;
                    }
                }
                catch (SSLException e)
                {
                    if (dst != null)
                    {
                        dst.free();
                    }
                    next.pipeline().shutdownInput();
                    return;
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP)
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    int bytesProduced = result.bytesProduced();
                    dst.addWritePosi(bytesProduced);
                    if (bytesProduced > 0)
                    {
                        next.pipeline().fireWrite(new ClientSSLProtocol().setData(dst));
                    }
                    else
                    {
                        dst.free();
                    }
                    dst = null;
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.CLOSED)
                    {
                        gracefulClose(next.pipeline());
                        return;
                    }
                }
                catch (SSLException e)
                {
                    if (dst != null)
                    {
                        dst.free();
                    }
                    next.pipeline().shutdownInput();
                    return;
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK)
            {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null)
                {
                    task.run();
                }
                hs = sslEngine.getHandshakeStatus();
            }
            else if (hs == SSLEngineResult.HandshakeStatus.FINISHED || hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            {
                handshakeFinished = true;
                handshakeLatch.countDown();
                handleData(next);
                return;
            }
        }
    }

    private void handleData(ReadProcessorNode next)
    {
        if (accumulation == null || accumulation.remainRead() == 0)
        {
            if (accumulation != null)
            {
                accumulation.free();
                accumulation = null;
            }
            return;
        }
        try
        {
            while (true)
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                SSLEngineResult result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                SSLEngineResult.Status status = result.getStatus();
                int bytesProduced = result.bytesProduced();
                accumulation.addReadPosi(result.bytesConsumed());
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    next.fireRead(dst);
                }
                else
                {
                    dst.free();
                }
                dst = null;
                if (status == SSLEngineResult.Status.OK)
                {
                    if (accumulation.remainRead() > 0)
                    {
                        continue;
                    }
                    else
                    {
                        accumulation.free();
                        accumulation = null;
                        return;
                    }
                }
                else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                {
                    accumulation.compact();
                    if (accumulation.capacity() < sslEngine.getSession().getPacketBufferSize())
                    {
                        accumulation.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
                    }
                    return;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    gracefulClose(next.pipeline());
                    return;
                }
            }
        }
        catch (SSLException e)
        {
            if (accumulation != null)
            {
                accumulation.free();
                accumulation = null;
            }
            next.pipeline().shutdownInput();
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        super.readFailed(e, next);
    }
}
