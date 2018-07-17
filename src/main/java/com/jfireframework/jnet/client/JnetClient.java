package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface JnetClient
{
	/**
	 * 执行数据写出，如果通道关闭或者写完成器已经处于停止状态等，会抛出异常.使用默认的阻塞策略执行。
	 * 
	 * @param packet
	 * @throws Exception
	 */
	void write(IoBuffer packet) throws Exception;
	
	/**
	 * 执行数据写出，如果通道关闭或者写完成器已经处于停止状态等，会抛出异常.<br/>
	 *
	 * @param packet
	 * @param block 为true时，会等待到数据写出或者抛出异常方法才会返回。为false时，数据会直接进入一个暂存队列，随后方法立刻返回。
	 * @throws Exception
	 */
	void write(IoBuffer packet, boolean block) throws Exception;
	
	void close();
	
	/**
	 * 执行数据写出时是否倾向于阻塞方式
	 * 
	 * @return
	 */
	boolean preferBlock();
}
