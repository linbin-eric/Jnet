package com.jfireframework.jnet.common.api;

public interface ProcessorInvoker
{
	/**
	 * 处理业务数据。如果自身或者下游到达容量瓶颈，无法处理该数据，则返回false。
	 * 
	 * @param data
	 * @throws Throwable
	 */
	boolean process(Object data) throws Throwable;
	
}
