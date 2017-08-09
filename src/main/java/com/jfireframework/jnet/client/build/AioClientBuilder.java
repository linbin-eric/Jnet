package com.jfireframework.jnet.client.build;

import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.ThreadFactory;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.client.client.AioClient;
import com.jfireframework.jnet.client.client.impl.ChannelAttachClient;
import com.jfireframework.jnet.client.client.impl.MutliAttachClient;
import com.jfireframework.jnet.client.client.impl.SimpleClient;
import com.jfireframework.jnet.client.client.impl.ThreadAttachClient;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.common.businessprocessor.MutlisAttachProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.util.DefaultAioListener;

public class AioClientBuilder
{
    public static enum ExecutorMode
    {
        FIX, CACHED
    }
    
    // 服务器的启动端口
    private int                          port                 = -1;
    /**
     * 处理socket事件的起始线程数。如果线程池模式选择固定线程数模式的话，则这个数值就是线程数的值。如果线程池模式选择cache模式的话，则这个数值是初始线程数。
     */
    private int                          ioThreadNum          = Runtime.getRuntime().availableProcessors();
    private ExecutorMode                 executorMode         = ExecutorMode.FIX;
    private String                       serverIp             = "0.0.0.0";
    private ThreadFactory                threadFactory;
    private AsynchronousChannelGroup     channelGroup;
    private AioListener                  aioListener;
    private ChannelContextBuilder        channelContextBuilder;
    private int                          businessProcessorNum = Runtime.getRuntime().availableProcessors();
    private IoMode                       ioMode               = IoMode.CHANNEL_ATTACH;
    private ExecutorService              executorService;
    private BlockingQueue<ProcessorTask> taskQueue;
    private MutlisAttachProcessor[]      processors;
    
    public synchronized AioClient build()
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
                        return new Thread(r, "客户端IO线程-" + (i++));
                    }
                };
            }
            if (channelGroup == null)
            {
                switch (executorMode)
                {
                    case FIX:
                        channelGroup = AsynchronousChannelGroup.withFixedThreadPool(ioThreadNum, threadFactory);
                        break;
                    case CACHED:
                        channelGroup = AsynchronousChannelGroup.withCachedThreadPool(Executors.newCachedThreadPool(threadFactory), ioThreadNum);
                        break;
                }
            }
            if (aioListener == null)
            {
                aioListener = new DefaultAioListener();
            }
            switch (ioMode)
            {
                case SIMPLE:
                    return new SimpleClient(executorService, channelContextBuilder, channelGroup, serverIp, port, aioListener);
                case THREAD_ATTACH:
                    if (taskQueue == null)
                    {
                        taskQueue = new LinkedTransferQueue<ProcessorTask>();
                    }
                    if (executorService == null)
                    {
                        executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                            int i = 1;
                            
                            @Override
                            public Thread newThread(Runnable r)
                            {
                                return new Thread(r, "客户端业务线程-THREAD_ATTACH-" + (i++));
                            }
                        });
                    }
                    return new ThreadAttachClient(executorService, channelContextBuilder, channelGroup, serverIp, port, aioListener);
                case CHANNEL_ATTACH:
                    if (executorService == null)
                    {
                        executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                            int i = 1;
                            
                            @Override
                            public Thread newThread(Runnable r)
                            {
                                return new Thread(r, "客户端业务线程-CHANNEL_ATTACH-" + (i++));
                            }
                        });
                    }
                    return new ChannelAttachClient(executorService, channelContextBuilder, channelGroup, serverIp, port, aioListener);
                case MUTLI_ATTACH:
                {
                    if (executorService == null)
                    {
                        int size = 1;
                        while (size < businessProcessorNum)
                        {
                            size = size << 1;
                        }
                        businessProcessorNum = size;
                        executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                            int i = 1;
                            
                            @Override
                            public Thread newThread(Runnable r)
                            {
                                return new Thread(r, "客户端业务线程-MUTLI_ATTACH-" + (i++));
                            }
                        });
                    }
                    if (processors == null)
                    {
                        processors = new MutlisAttachProcessor[businessProcessorNum];
                        for (int i = 0; i < businessProcessorNum; i++)
                        {
                            MutlisAttachProcessor processor = new MutlisAttachProcessor(aioListener);
                            processors[i] = processor;
                            executorService.execute(processor);
                        }
                    }
                    return new MutliAttachClient(processors, executorService, channelContextBuilder, channelGroup, serverIp, port, aioListener);
                }
                default:
                    throw new NullPointerException();
            }
        }
        catch (Throwable e)
        {
            throw new JustThrowException(e);
        }
    }
    
    public void setBusinessThreadNum(int businessThreadNum)
    {
        this.businessProcessorNum = businessThreadNum;
    }
    
    public void setPort(int port)
    {
        this.port = port;
    }
    
    public void setIoThreadNum(int ioThreadNum)
    {
        this.ioThreadNum = ioThreadNum;
    }
    
    public void setExecutorMode(ExecutorMode executorMode)
    {
        this.executorMode = executorMode;
    }
    
    public void setServerIp(String serverIp)
    {
        this.serverIp = serverIp;
    }
    
    public void setThreadFactory(ThreadFactory threadFactory)
    {
        this.threadFactory = threadFactory;
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
    
    public void setIoMode(IoMode ioMode)
    {
        this.ioMode = ioMode;
    }
    
    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }
    
    public void setTaskQueue(BlockingQueue<ProcessorTask> taskQueue)
    {
        this.taskQueue = taskQueue;
    }
    
}
