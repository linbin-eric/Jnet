package com.jfireframework.jnet.client;

import com.jfireframework.pool.ioBuffer.IoBuffer;

public interface AioClient
{
	
	public void connect();
	
	public void close();
	
	public void write(IoBuffer packet);
	
}
