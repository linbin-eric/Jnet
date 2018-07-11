package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface ChannelContext
{
    /**
     * 向通道写出数据
     * 
     * @param buffer
     * @throws Throwable
     */
    void write(IoBuffer buffer);
    
    /**
     * 处理数据
     * 
     * @param buffer
     * @throws Throwable
     */
    void process(IoBuffer buffer) throws Throwable;
    
    /**
     * 设置数据处理器
     * 
     * @param dataProcessors
     */
    void setDataProcessor(DataProcessor<?>... dataProcessors);
    
    AsynchronousSocketChannel socketChannel();
}
