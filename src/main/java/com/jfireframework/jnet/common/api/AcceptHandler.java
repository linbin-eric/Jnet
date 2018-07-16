package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public interface AcceptHandler extends CompletionHandler<AsynchronousSocketChannel, AsynchronousServerSocketChannel>
{
	
}
