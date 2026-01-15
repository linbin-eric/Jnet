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
public class ClientSSLEncoder implements WriteProcessor<IoBuffer>
{
    private final SSLEngine sslEngine;
    private       String    remote;

    @Override
    public void write(IoBuffer buffer, WriteProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        BufferAllocator allocator = next.pipeline().allocator();
        ByteBuffer      src       = buffer.readableByteBuffer();
        while (true)
        {
            IoBuffer dst = null;
            try
            {
                dst = allocator.allocate(sslEngine.getSession().getPacketBufferSize());
                SSLEngineResult        result        = sslEngine.wrap(src, dst.writableByteBuffer());
                SSLEngineResult.Status status        = result.getStatus();
                int                    bytesProduced = result.bytesProduced();
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
                    buffer.free();
                    return;
                }
            }
            catch (SSLException e)
            {
                if (dst != null)
                {
                    dst.free();
                }
                buffer.free();
                return;
            }
        }
    }
}
