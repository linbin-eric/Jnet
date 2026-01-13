package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.ChannelConfig;

import java.nio.channels.AsynchronousSocketChannel;

public interface Pipeline
{
    /**
     * 走完整的写出责任链
     *
     * @param data
     */
    void fireWrite(Object data);

    /**
     * 直接提交数据到写出通道，不走责任链
     *
     * @param buffer
     */
    void directWrite(IoBuffer buffer);

    void addReadProcessor(ReadProcessor<?> processor);

    void addWriteProcessor(WriteProcessor<?> processor);

    void shutdownInput();

    AsynchronousSocketChannel socketChannel();

    ChannelConfig channelConfig();

    Object getAttach();

    void setAttach(Object attach);

    void setWriteListener(WriteListener writeListener);

    boolean isOpen();

    default String getRemoteAddressWithoutException()
    {
        try
        {
            return socketChannel().getRemoteAddress().toString();
        }
        catch (Throwable e)
        {
            return null;
        }
    }

    BufferAllocator allocator();

    String pipelineId();
}
