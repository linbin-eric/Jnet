package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.nio.channels.AsynchronousSocketChannel;

public interface ChannelContext
{
    /**
     * 向通道放入待发送数据，如果还存在发送空间，则放入成功，并且返回true。如果发送空间当前已满，则返回false。
     *
     * @param buffer
     * @throws Throwable
     */
    void write(IoBuffer buffer);

    /**
     * 设置数据处理器.容器会自动在末尾添加一个处理器，该处理的实现仅仅是调用了write接口。
     *
     * @param dataProcessors
     */
    void setDataProcessor(DataProcessor<?>... dataProcessors);

    AsynchronousSocketChannel socketChannel();

    void close();

    void close(Throwable e);
}
