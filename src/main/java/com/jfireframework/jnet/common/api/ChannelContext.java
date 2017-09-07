package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;

public interface ChannelContext
{
	
	/**
	 * 与当前通道关联的一个附属对象
	 * 
	 * @return
	 */
	Object attachment();
	
	SendBufStorage sendBufStorage();
	
	void registerRead();
	
	void registerWrite();
	
	/**
	 * 推送一个数据到通道中准备发送
	 * 
	 * @param send
	 * @param index
	 * @throws Throwable
	 */
	void push(Object send, int index) throws Throwable;
	
	/**
	 * 注意，方法的内部实现保证close方法实际上只会被调用一次。返回true意味着真正的调用了close方法。返回false，就意味着有别人已经调用了close方法
	 */
	boolean close();
	
	/**
	 * 从通道接收到数据后，进行对应处理的处理器
	 * 
	 * @return
	 */
	StreamProcessor[] inProcessors();;
	
	/**
	 * 向一个通道发送一个数据，进行对应处理的处理器
	 * 
	 * @return
	 */
	StreamProcessor[] outProcessors();
	
	AsynchronousSocketChannel socketChannel();
	
	boolean isOpen();
}
