package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Data
public class ClientSSLEncoder implements WriteProcessor<Object>
{
    private final SSLEngine         sslEngine;
    private final ClientSSLDecoder  sslDecoder;
    private       String            remote;

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (data instanceof ClientSSLProtocol sslProtocol)
        {
            if (sslProtocol.isCloseNotify())
            {
                handleCloseNotify(next);
            }
            else if (sslProtocol.isStartHandshake())
            {
                handleStartHandshake(next);
            }
            else if (sslProtocol.getData() != null)
            {
                next.fireWrite(sslProtocol.getData());
            }
        }
        else if (data instanceof IoBuffer buffer)
        {
            wrapAndSend(buffer, next);
        }
        else
        {
            next.fireWrite(data);
        }
    }

    private void handleStartHandshake(WriteProcessorNode next)
    {
        BufferAllocator allocator = next.pipeline().allocator();
        while (true)
        {
            IoBuffer dst = null;
            try
            {
                dst = allocator.allocate(sslEngine.getSession().getPacketBufferSize());
                SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                int bytesProduced = result.bytesProduced();
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    next.fireWrite(dst);
                }
                else
                {
                    dst.free();
                }
                dst = null;
                SSLEngineResult.HandshakeStatus hs = result.getHandshakeStatus();
                if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                {
                    continue;
                }
                return;
            }
            catch (SSLException e)
            {
                if (dst != null)
                {
                    dst.free();
                }
                sslDecoder.gracefulClose(next.pipeline());
                return;
            }
        }
    }

    private void handleCloseNotify(WriteProcessorNode next)
    {
        while (true)
        {
            IoBuffer dst = null;
            try
            {
                dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
                SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                int bytesProduced = result.bytesProduced();
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    next.fireWrite(dst);
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
                    return;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    return;
                }
            }
            catch (SSLException e)
            {
                if (dst != null)
                {
                    dst.free();
                }
                sslDecoder.gracefulClose(next.pipeline());
                return;
            }
        }
    }

    private void wrapAndSend(IoBuffer buffer, WriteProcessorNode next)
    {
        BufferAllocator allocator = next.pipeline().allocator();
        ByteBuffer src = buffer.readableByteBuffer();
        while (true)
        {
            IoBuffer dst = null;
            try
            {
                dst = allocator.allocate(sslEngine.getSession().getPacketBufferSize());
                SSLEngineResult result = sslEngine.wrap(src, dst.writableByteBuffer());
                SSLEngineResult.Status status = result.getStatus();
                int bytesProduced = result.bytesProduced();
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    next.fireWrite(dst);
                }
                else
                {
                    dst.free();
                }
                dst = null;
                if (status == SSLEngineResult.Status.OK)
                {
                    if (src.hasRemaining())
                    {
                        continue;
                    }
                    else
                    {
                        buffer.free();
                        return;
                    }
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    sslDecoder.gracefulClose(next.pipeline());
                    return;
                }
            }
            catch (SSLException e)
            {
                if (dst != null)
                {
                    dst.free();
                }
                sslDecoder.gracefulClose(next.pipeline());
                return;
            }
        }
    }
}
