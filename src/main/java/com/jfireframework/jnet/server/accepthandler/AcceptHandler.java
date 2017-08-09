package com.jfireframework.jnet.server.accepthandler;

import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public interface AcceptHandler extends CompletionHandler<AsynchronousSocketChannel, Object>
{
    public void stop();
}
