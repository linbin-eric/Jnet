package com.jfireframework.jnet.server.build;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.util.DefaultAioListener;
import com.jfireframework.jnet.server.AioServer;

public class AioServerBuilder
{
    public static enum ExecutorMode
    {
        FIX, CACHED
    }
    
    private AioListener              aioListener;
    private ChannelContextBuilder    channelContextBuilder;
    // 服务器的启动端口
    private int                      port                 = -1;
    /**
     * 处理socket事件的起始线程数。如果线程池模式选择固定线程数模式的话，则这个数值就是线程数的值。如果线程池模式选择cache模式的话，则这个数值是初始线程数。
     */
    private int                      ioProcessorNum       = Runtime.getRuntime().availableProcessors();
    private int                      businessProcessorNum = Runtime.getRuntime().availableProcessors();
    private ExecutorMode             executorMode         = ExecutorMode.FIX;
    private IoMode                   ioMode               = IoMode.SIMPLE;
    private String                   bindIp               = "0.0.0.0";
    private ThreadFactory            threadFactory;
    private AsynchronousChannelGroup channelGroup;
    
    public AioServer build()
    {
        try
        {
            if (threadFactory == null)
            {
                threadFactory = new ThreadFactory() {
                    int i = 1;
                    
                    @Override
                    public Thread newThread(Runnable r)
                    {
                        return new Thread(r, "服务端IO线程-" + (i++));
                    }
                };
            }
            if (channelGroup == null)
            {
                switch (executorMode)
                {
                    case FIX:
                        channelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioProcessorNum, threadFactory);
                        break;
                    case CACHED:
                        channelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(threadFactory), ioProcessorNum);
                        break;
                }
            }
            if (aioListener == null)
            {
                aioListener = new DefaultAioListener();
            }
            AioServer aioServer = new AioServer(businessProcessorNum, bindIp, port, channelGroup, ioMode, channelContextBuilder, aioListener);
            return aioServer;
        }
        catch (Throwable e)
        {
            throw new JustThrowException(e);
        }
    }
    
    public IoMode getIoMode()
    {
        return ioMode;
    }
    
    public void setIoMode(IoMode ioMode)
    {
        this.ioMode = ioMode;
    }
    
    public void setChannelContextBuilder(ChannelContextBuilder channelContextBuilder)
    {
        this.channelContextBuilder = channelContextBuilder;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public void setBindIp(String bindIp)
    {
        this.bindIp = bindIp;
    }
    
    public void setThreadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
    }
    
    public void setChannelGroup(AsynchronousChannelGroup channelGroup)
    {
        this.channelGroup = channelGroup;
    }
    
    public void setIoProcessorNum(int ioProcessorNum)
    {
        this.ioProcessorNum = ioProcessorNum;
    }
    
    public void setBusinessProcessorNum(int businessProcessorNum)
    {
        this.businessProcessorNum = businessProcessorNum;
    }
    
    public void setExecutorMode(ExecutorMode executorMode)
    {
        this.executorMode = executorMode;
    }
    
    public void setAioListener(AioListener aioListener)
    {
        this.aioListener = aioListener;
    }
    
}
