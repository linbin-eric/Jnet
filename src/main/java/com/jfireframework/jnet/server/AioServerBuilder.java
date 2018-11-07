package com.jfireframework.jnet.server;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AcceptHandler;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.internal.DefaultAioListener;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;

public class AioServerBuilder
{
    private AioListener              aioListener;
    private int                      port   = -1;
    private String                   bindIp = "0.0.0.0";
    private AsynchronousChannelGroup channelGroup;
    private AcceptHandler            acceptHandler;

    public AioServer build()
    {
        try
        {
            if (channelGroup == null)
            {
                channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2 + 1, new ThreadFactory()
                {
                    int i = 1;

                    @Override
                    public Thread newThread(Runnable r)
                    {
                        return new FastThreadLocalThread(r, "AioServer_IoWorker-" + (i++));
                    }
                });
            }
            if (aioListener == null)
            {
                aioListener = new DefaultAioListener();
            }
            if (acceptHandler == null)
            {
                throw new NullPointerException("AcceptHandler 没有赋值");
            }
            AioServer aioServer = new AioServer(bindIp, port, channelGroup, acceptHandler);
            return aioServer;
        } catch (Throwable e)
        {
            ReflectUtil.throwException(e);
            return null;
        }
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public void setBindIp(String bindIp)
    {
        this.bindIp = bindIp;
    }

    public void setChannelGroup(AsynchronousChannelGroup channelGroup)
    {
        this.channelGroup = channelGroup;
    }

    public void setAioListener(AioListener aioListener)
    {
        this.aioListener = aioListener;
    }

    public AioServerBuilder setAcceptHandler(AcceptHandler acceptHandler)
    {
        this.acceptHandler = acceptHandler;
        return this;
    }
}
