package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface AioClient
{
	/**
	 * 执行数据写出，如果通道关闭或者写完成器已经处于停止状态等，会抛出异常
	 * 
	 * @param packet
	 * @throws Exception
	 */
	void write(IoBuffer packet) throws Exception;
	
	void close();
}
