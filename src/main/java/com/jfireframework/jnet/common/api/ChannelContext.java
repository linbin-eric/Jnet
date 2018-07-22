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
     * 以背压方式向通道写出数据。如果写完成器没有足够空间容纳该数据，返回false。
     * 
     * @param buffer
     * @return
     */
    boolean backPressureWrite(IoBuffer buffer);
    
    /**
     * 处理读完成器读取到的数据。<br/>
     * 注意:该过程不可以对buffer执行任何的free操作。因为该Buffer后续还需要继续给读完成器使用
     * 
     * @param buffer
     * @throws Throwable
     */
    void process(IoBuffer buffer) throws Throwable;
    
    /**
     * 以背压方式处理读完成器读取到的数据.如果处理器到达容量瓶颈无法处理，则返回false。此时读完成器应该放弃注册自身读。<br/>
     * 注意:该过程不可以对buffer执行任何的free操作。因为该Buffer后续还需要继续给读完成器使用
     * 
     * @param buffer
     * @return
     * @throws Throwable
     */
    boolean backPressureProcess(IoBuffer buffer) throws Throwable;
    
    /**
     * 设置数据处理器
     * 
     * @param dataProcessors
     */
    void setDataProcessor(DataProcessor<?>... dataProcessors);
    
    AsynchronousSocketChannel socketChannel();
    
    /**
     * 绑定关联的写完成器实例
     * 
     * @param writeCompletionHandler
     */
    void bind(WriteCompletionHandler writeCompletionHandler);
}
