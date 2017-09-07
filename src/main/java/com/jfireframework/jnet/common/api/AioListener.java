package com.jfireframework.jnet.common.api;

public interface AioListener
{
	/**
	 * 当数据被写出后触发
	 * 
	 * @param channelContext
	 * @param writes
	 */
	void afterWrited(ChannelContext channelContext, int writes);
	
	/**
	 * 通道发生异常时触发
	 * 
	 * @param e
	 * @param channelContext
	 */
	void catchException(Throwable e, ChannelContext channelContext);
	
	/**
	 * 当通道注册读取时触发
	 * 
	 * @param channelContext
	 */
	void readRegister(ChannelContext channelContext);
	
	/**
	 * 通道收到消息后触发
	 * 
	 * @param context
	 */
	void afterReceived(ChannelContext channelContext);
	
}
