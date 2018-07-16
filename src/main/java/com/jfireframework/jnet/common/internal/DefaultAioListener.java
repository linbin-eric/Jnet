package com.jfireframework.jnet.common.internal;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;

public class DefaultAioListener implements AioListener
{
    
    @Override
    public void catchException(Throwable e, AsynchronousSocketChannel socketChannel)
    {
        if (e instanceof AsynchronousCloseException == false)
        {
            e.printStackTrace();
        }
        try
        {
            socketChannel.close();
        }
        catch (IOException e1)
        {
            e1.printStackTrace();
        }
    }
    
    @Override
    public void afterWrited(AsynchronousSocketChannel socketChannel, Integer writes)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void onAccept(AsynchronousSocketChannel socketChannel, ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void afterReceived(AsynchronousSocketChannel socketChannel)
    {
        // TODO Auto-generated method stub
        
    }
    
}
