package com.jfireframework.jnet.common.api;

public interface StreamProcessor
{
	
	/**
	 * 对传递过来的数据做处理。并且将处理完成的结果返回。后续的处理器会继续处理这个对象
	 * 
	 * @param data
	 * @param entry
	 * @throws Throwable 如果方法抛出了异常，则首先会执行捕获异常的动作。然后关闭该通道
	 */
	public Object process(Object data, ChannelContext context) throws Throwable;
	
}
