package com.jfireframework.jnet.common.api;

public interface ChannelConnectInitializer
{
	/**
	 * 当链接初始化的时候触发
	 */
	void initChannelContext(ChannelContext channelContext);
	
}
