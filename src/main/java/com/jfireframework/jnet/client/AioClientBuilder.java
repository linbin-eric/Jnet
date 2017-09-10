package com.jfireframework.jnet.client;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContextBuilder;
import com.jfireframework.jnet.common.support.DefaultAioListener;

public class AioClientBuilder
{
    
    // 服务器的启动端口
    private int                      port     = -1;
    /**
     * 处理socket事件的起始线程数。如果线程池模式选择固定线程数模式的话，则这个数值就是线程数的值。如果线程池模式选择cache模式的话，则这个数值是初始线程数。
     */
    private String                   serverIp = "0.0.0.0";
    private AsynchronousChannelGroup channelGroup;
    private AioListener              aioListener;
    private ChannelContextBuilder    channelContextBuilder;
    
    public synchronized AioClient build()
    {
        try
        {
            if (channelContextBuilder == null)
            {
                throw new NullPointerException();
            }
            if (channelGroup == null)
            {
                channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {
                    int i = 1;
                    
                    @Override
                    public Thread newThread(Runnable r)
                    {
                        return new Thread(r, "客户端IO线程-" + (i++));
                    }
                });
            }
            if (aioListener == null)
            {
                aioListener = new DefaultAioListener();
            }
            return new DefaultClient(channelContextBuilder, channelGroup, serverIp, port, aioListener);
        }
        catch (Throwable e)
        {
            throw new JustThrowException(e);
        }
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public void setServerIp(String serverIp)
    {
        this.serverIp = serverIp;
    }
    
    public void setChannelGroup(AsynchronousChannelGroup channelGroup)
    {
        this.channelGroup = channelGroup;
    }
    
    public void setAioListener(AioListener aioListener)
    {
        this.aioListener = aioListener;
    }
    
    public void setChannelContextBuilder(ChannelContextBuilder channelContextBuilder)
    {
        this.channelContextBuilder = channelContextBuilder;
    }
    
}
