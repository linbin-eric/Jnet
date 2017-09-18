package com.jfireframework.jnet.client;

public interface AioClient
{
	
	public void connect();
	
	public void close();
	
	public void write(Object packet) throws Throwable;
	
}
