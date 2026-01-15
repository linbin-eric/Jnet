package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Data
@EqualsAndHashCode(callSuper = true)
public class AbstractSSLDecoder extends AbstractDecoder
{
    protected final    SSLEngine sslEngine;
    protected          boolean   handshakeFinished = false;
    protected volatile boolean   closeInbound      = false;
    protected          String    remote;

    public AbstractSSLDecoder(SSLEngine sslEngine)
    {
        this.sslEngine = sslEngine;
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
        }
        sslEngine.closeOutbound();
        while (true)
        {
            IoBuffer dst = pipeline.allocator().allocate(sslEngine.getSession().getPacketBufferSize());
            try
            {
                SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                int bytesProduced = result.bytesProduced();
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    pipeline.directWrite(dst);
                }
                else
                {
                    dst.free();
                }
                dst = null;
                SSLEngineResult.Status status = result.getStatus();
                if (status == SSLEngineResult.Status.OK)
                {
                    if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                    {
                        continue;
                    }
                    break;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    break;
                }
            }
            catch (SSLException e)
            {
                if (dst != null)
                {
                    dst.free();
                }
                break;
            }
        }
        pipeline.shutdownInput();
    }

    protected void handshake(ReadProcessorNode next, SSLEngineResult.HandshakeStatus hs)
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
                        next.pipeline().directWrite(dst);
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
            else if (hs == SSLEngineResult.HandshakeStatus.FINISHED)
            {
                handshakeFinished = true;
                onHandshakeFinished();
                handleData(next);
                return;
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            {
                // 保守方案：NOT_HANDSHAKING 在握手过程中出现视为异常，关闭连接
                gracefulClose(next.pipeline());
                return;
            }
        }
    }

    protected void handleData(ReadProcessorNode next)
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

    /**
     * 握手完成时的回调，子类可覆盖
     */
    protected void onHandshakeFinished()
    {
        // 默认空实现
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
