package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.util.AioListenerAdapter;

public class DefaultAioListener extends AioListenerAdapter
{

    @Override
    public void catchException(Throwable e, ChannelContext channelContext)
    {
        channelContext.close(e);
    }
}
