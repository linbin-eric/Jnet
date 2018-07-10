package com.jfireframework.jnet.common.api;

public interface DataProcessor<T>
{
	/**
	 * 通道初始化时被调用
	 * 
	 * @param channelContext
	 */
	void initialize(ChannelContext channelContext);
	
	/**
	 * 处理由上一个Invoker传递过来的数据
	 * 
	 * @param data
	 * @param next
	 * @throws Throwable
	 */
	void process(T data, ProcessorInvoker next) throws Throwable;
}
