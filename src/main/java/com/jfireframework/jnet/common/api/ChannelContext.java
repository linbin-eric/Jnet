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
	 * 处理读完成器读取到的数据。<br/>
	 * 注意:该过程不可以对buffer执行任何的free操作。因为该Buffer后续还需要继续给读完成器使用
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
