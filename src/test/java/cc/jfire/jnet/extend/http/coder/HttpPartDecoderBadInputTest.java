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

public class HttpPartDecoderBadInputTest
{
    @Test
    public void malformedRequestHeaderClosesConnectionInsteadOfThrowing()
    {
        RecordingNode next    = new RecordingNode();
        IoBuffer      request = buffer("GET / HTTP/1.1\r\nBrokenHeader\r\n\r\n");

        new HttpRequestPartDecoder().read(request, next);

        Assert.assertEquals(0, next.readCount);
        Assert.assertEquals(1, next.failedCount);
        Assert.assertTrue(next.failure instanceof IllegalArgumentException);
        Assert.assertTrue(next.pipeline.shutdown);
    }

    @Test
    public void malformedResponseHeaderClosesConnectionInsteadOfThrowing()
    {
        RecordingNode next     = new RecordingNode();
        IoBuffer      response = buffer("HTTP/1.1 200 OK\r\nBrokenHeader\r\n\r\n");

        new HttpResponsePartDecoder().read(response, next);

        Assert.assertEquals(0, next.readCount);
        Assert.assertEquals(1, next.failedCount);
        Assert.assertTrue(next.failure instanceof IllegalArgumentException);
        Assert.assertTrue(next.pipeline.shutdown);
    }

    private static IoBuffer buffer(String content)
    {
        byte[]   bytes  = content.getBytes(StandardCharsets.UTF_8);
        IoBuffer buffer = UnPoolBufferAllocator.DEFAULT.allocate(bytes.length);
        buffer.put(bytes);
        return buffer;
    }

    private static class RecordingNode implements ReadProcessorNode
    {
        private final RecordingPipeline pipeline = new RecordingPipeline();
        private       int               readCount;
        private       int               failedCount;
        private       Throwable         failure;

        @Override
        public void fireRead(Object data)
        {
            readCount++;
        }

        @Override
        public void fireReadFailed(Throwable e)
        {
            failedCount++;
            failure = e;
        }

        @Override
        public void fireReadCompleted()
        {
        }

        @Override
        public void firePipelineComplete(Pipeline pipeline)
        {
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
        private       boolean         shutdown;
        private       Object          attach;

        @Override
        public void fireWrite(Object data)
        {
        }

        @Override
        public void directWrite(IoBuffer buffer)
        {
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
            shutdown = true;
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
            return attach;
        }

        @Override
        public void setAttach(Object attach)
        {
            this.attach = attach;
        }

        @Override
        public void setWriteListener(WriteListener writeListener)
        {
        }

        @Override
        public boolean isOpen()
        {
            return !shutdown;
        }

        @Override
        public BufferAllocator allocator()
        {
            return allocator;
        }

        @Override
        public String pipelineId()
        {
            return "test-pipeline";
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
