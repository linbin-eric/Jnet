package com.jfireframework.jnet.common.build;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.ChannelContext;

public interface ChannelContextBuilder
{
    /**
     * 当客户端连接建立的时候触发
     */
    public ChannelContextConfig onConnect(AsynchronousSocketChannel socketChannel);
    
    public void afterContextBuild(ChannelContext serverChannelContext);
}
