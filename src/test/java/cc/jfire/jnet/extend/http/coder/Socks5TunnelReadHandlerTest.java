package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.api.WriteListener;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.allocator.impl.UnPoolBufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.ChannelConfig;
import org.junit.Assert;
import org.junit.Test;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Socks5TunnelReadHandlerTest
{
    @Test
    public void noAuthDomainConnectAndRemainingData() throws Exception
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("example.com", 443, null, null);

        handler.pipelineComplete(next.pipeline, next);
        Assert.assertArrayEquals(bytes(0x05, 0x01, 0x00), next.pipeline.writes.get(0));

        handler.read(buffer(0x05, 0x00), next);
        Assert.assertEquals(2, next.pipeline.writes.size());
        byte[] connect = next.pipeline.writes.get(1);
        Assert.assertEquals(0x05, connect[0] & 0xff);
        Assert.assertEquals(0x01, connect[1] & 0xff);
        Assert.assertEquals(0x00, connect[2] & 0xff);
        Assert.assertEquals(0x03, connect[3] & 0xff);
        Assert.assertEquals("example.com".length(), connect[4] & 0xff);
        Assert.assertEquals(443, ((connect[connect.length - 2] & 0xff) << 8) | (connect[connect.length - 1] & 0xff));

        handler.read(buffer(0x05, 0x00, 0x00, 0x01, 127, 0, 0, 1, 0x1f, 0x90, 'H', 'T', 'T', 'P'), next);

        Assert.assertTrue(handler.isTunnelEstablished());
        Assert.assertTrue(handler.awaitTunnelEstablished(1, TimeUnit.SECONDS));
        Assert.assertEquals(1, next.pipelineCompleteCount);
        Assert.assertEquals(1, next.reads.size());
        Assert.assertArrayEquals("HTTP".getBytes(StandardCharsets.US_ASCII), next.reads.get(0));
    }

    @Test
    public void userPassAuthSendsCredentialThenConnect()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("127.0.0.1", 8080, "user", "pass");

        handler.pipelineComplete(next.pipeline, next);
        Assert.assertArrayEquals(bytes(0x05, 0x01, 0x02), next.pipeline.writes.get(0));

        handler.read(buffer(0x05, 0x02), next);
        Assert.assertArrayEquals(new byte[]{0x01, 0x04, 'u', 's', 'e', 'r', 0x04, 'p', 'a', 's', 's'}, next.pipeline.writes.get(1));

        handler.read(buffer(0x01, 0x00), next);
        byte[] connect = next.pipeline.writes.get(2);
        Assert.assertEquals(0x01, connect[3] & 0xff);
        Assert.assertArrayEquals(bytes(127, 0, 0, 1), new byte[]{connect[4], connect[5], connect[6], connect[7]});
        Assert.assertEquals(8080, ((connect[connect.length - 2] & 0xff) << 8) | (connect[connect.length - 1] & 0xff));
    }

    @Test
    public void ipv6TargetUsesIpv6AddressType()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("::1", 443, null, null);

        handler.pipelineComplete(next.pipeline, next);
        handler.read(buffer(0x05, 0x00), next);

        byte[] connect = next.pipeline.writes.get(1);
        Assert.assertEquals(0x04, connect[3] & 0xff);
        Assert.assertEquals(22, connect.length);
    }

    @Test
    public void connectResponseCanArriveInPieces()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("example.com", 80, null, null);

        handler.pipelineComplete(next.pipeline, next);
        handler.read(buffer(0x05, 0x00), next);
        handler.read(buffer(0x05, 0x00, 0x00, 0x03, 0x03, 'a'), next);

        Assert.assertFalse(handler.isTunnelEstablished());

        handler.read(buffer('b', 'c', 0x00, 0x50), next);

        Assert.assertTrue(handler.isTunnelEstablished());
        Assert.assertEquals(1, next.pipelineCompleteCount);
    }

    @Test
    public void unsupportedAuthFails()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("example.com", 80, null, null);

        handler.pipelineComplete(next.pipeline, next);
        handler.read(buffer(0x05, 0xff), next);

        Assert.assertNotNull(handler.getTunnelError());
        Assert.assertFalse(handler.isTunnelEstablished());
    }

    @Test
    public void userPassAuthFailureFails()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("example.com", 80, "user", "pass");

        handler.pipelineComplete(next.pipeline, next);
        handler.read(buffer(0x05, 0x02), next);
        handler.read(buffer(0x01, 0x01), next);

        Assert.assertNotNull(handler.getTunnelError());
        Assert.assertFalse(handler.isTunnelEstablished());
    }

    @Test
    public void connectRejectFails()
    {
        RecordingNode          next    = new RecordingNode();
        Socks5TunnelReadHandler handler = new Socks5TunnelReadHandler("example.com", 80, null, null);

        handler.pipelineComplete(next.pipeline, next);
        handler.read(buffer(0x05, 0x00), next);
        handler.read(buffer(0x05, 0x05, 0x00, 0x01, 127, 0, 0, 1, 0, 0), next);

        Assert.assertNotNull(handler.getTunnelError());
        Assert.assertFalse(handler.isTunnelEstablished());
    }

    private static IoBuffer buffer(int... values)
    {
        IoBuffer buffer = UnPoolBufferAllocator.DEFAULT.allocate(values.length);
        for (int value : values)
        {
            buffer.put((byte) value);
        }
        return buffer;
    }

    private static byte[] bytes(int... values)
    {
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++)
        {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    private static byte[] take(IoBuffer buffer)
    {
        byte[] bytes = new byte[buffer.remainRead()];
        buffer.get(bytes);
        buffer.free();
        return bytes;
    }

    private static class RecordingNode implements ReadProcessorNode
    {
        private final RecordingPipeline pipeline = new RecordingPipeline();
        private final List<byte[]>      reads    = new ArrayList<>();
        private       int               pipelineCompleteCount;

        @Override
        public void fireRead(Object data)
        {
            if (data instanceof IoBuffer buffer)
            {
                reads.add(take(buffer));
            }
        }

        @Override
        public void fireReadFailed(Throwable e)
        {
        }

        @Override
        public void fireReadCompleted()
        {
        }

        @Override
        public void firePipelineComplete(Pipeline pipeline)
        {
            pipelineCompleteCount++;
        }

        @Override
        public ReadProcessorNode getNext()
        {
            return null;
        }

        @Override
        public void setNext(ReadProcessorNode next)
        {
        }

        @Override
        public Pipeline pipeline()
        {
            return pipeline;
        }
    }

    private static class RecordingPipeline implements Pipeline
    {
        private final BufferAllocator allocator     = UnPoolBufferAllocator.DEFAULT;
        private final ChannelConfig   channelConfig = new ChannelConfig().setAllocatorSupplier(() -> allocator);
        private final List<byte[]>    writes        = new ArrayList<>();

        @Override
        public void fireWrite(Object data)
        {
        }

        @Override
        public void directWrite(IoBuffer buffer)
        {
            writes.add(take(buffer));
        }

        @Override
        public void addReadProcessor(ReadProcessor<?> processor)
        {
        }

        @Override
        public void addWriteProcessor(WriteProcessor<?> processor)
        {
        }

        @Override
        public void shutdownInput()
        {
        }

        @Override
        public AsynchronousSocketChannel socketChannel()
        {
            return null;
        }

        @Override
        public ChannelConfig channelConfig()
        {
            return channelConfig;
        }

        @Override
        public Object getAttach()
        {
            return null;
        }

        @Override
        public void setAttach(Object attach)
        {
        }

        @Override
        public void setWriteListener(WriteListener writeListener)
        {
        }

        @Override
        public boolean isOpen()
        {
            return true;
        }

        @Override
        public BufferAllocator allocator()
        {
            return allocator;
        }

        @Override
        public String pipelineId()
        {
            return "socks5-test";
        }

        @Override
        public void putPersistenceStore(String key, Object value)
        {
        }

        @Override
        public Object getPersistenceStore(String key)
        {
            return null;
        }
    }
}
