package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.util.AioListenerAdapter;

import java.io.IOException;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;

public class DefaultAioListener extends AioListenerAdapter
{

    @Override
    public void catchException(Throwable e, ChannelContext channelContext)
    {
        if (e instanceof AsynchronousCloseException == false)
        {
            channelContext.close(e);
        }
    }
}
