package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Data
@EqualsAndHashCode(callSuper = true)
public class ClientSSLDecoder extends AbstractSSLDecoder
{
    private final CountDownLatch handshakeLatch = new CountDownLatch(1);

    public ClientSSLDecoder(SSLEngine sslEngine)
    {
        super(sslEngine);
    }

    public boolean waitHandshake(long timeout, TimeUnit unit) throws InterruptedException
    {
        return handshakeLatch.await(timeout, unit);
    }

    public void startHandshake(Pipeline pipeline)
    {
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
                gracefulClose(pipeline);
                return;
            }
        }
    }

    @Override
    protected void onHandshakeFinished()
    {
        handshakeLatch.countDown();
    }
}
