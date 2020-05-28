package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.util.AioListenerAdapter;

public class DefaultAioListener extends AioListenerAdapter
{

    @Override
    public void catchException(Throwable e, ChannelContext channelContext)
    {
        channelContext.close(e);
    }
}
