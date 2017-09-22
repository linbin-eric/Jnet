package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;

public interface ChannelContext
{
	
	/**
	 * 与当前通道关联的一个附属对象
	 * 
	 * @return
	 */
	Object getAttachment();
	
	/**
	 * 设置关联的附属对象
	 * 
	 * @param attachment
	 */
	void setAttachment(Object attachment);
	
	SendBufStorage sendBufStorage();
	
	/**
	 * 处理从通道读取到的报文
	 * 
	 * @param packet
	 * @throws Throwable
	 */
	void process(ByteBuf<?> packet) throws Throwable;
	
	void registerRead();
	
	void registerWrite();
	
	/**
	 * 推送一个数据到通道中准备发送
	 * 
	 * @param send
	 * @throws Throwable
	 */
	void push(Object send) throws Throwable;
	
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
	
	ByteBuf<?> inCachedBuf();
	
	ByteBuf<?> outCachedBuf();
	
	FrameDecodec frameDecodec();
	
	int maxMerge();
}
