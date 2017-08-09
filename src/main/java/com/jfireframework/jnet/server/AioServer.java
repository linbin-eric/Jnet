package com.jfireframework.jnet.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import com.jfireframework.baseutil.exception.JustThrowException;
import com.jfireframework.jnet.common.IoMode;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.build.ChannelContextBuilder;
import com.jfireframework.jnet.server.accepthandler.AcceptHandler;
import com.jfireframework.jnet.server.accepthandler.impl.ChannelAttachAcceptHandler;
import com.jfireframework.jnet.server.accepthandler.impl.MutliAttachAcceptHandler;
import com.jfireframework.jnet.server.accepthandler.impl.SimpleAcceptHandler;
import com.jfireframework.jnet.server.accepthandler.impl.ThreadAttachAcceptHandler;

public class AioServer
{
    private AsynchronousChannelGroup        channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private String                          ip;
    private int                             port;
    private AcceptHandler                   acceptHandler;
    private IoMode                          ioMode;
    private ChannelContextBuilder           serverChannelContextBuilder;
    private int                             businessProcessorNum;
    private AioListener                     serverListener;
    
    public AioServer(int businessProcessorNum, String ip, int port, AsynchronousChannelGroup channelGroup, IoMode ioMode, ChannelContextBuilder serverChannelContextBuilder, AioListener serverListener)
    {
        this.businessProcessorNum = businessProcessorNum;
        this.ip = ip;
        this.port = port;
        this.channelGroup = channelGroup;
        this.ioMode = ioMode;
        this.serverChannelContextBuilder = serverChannelContextBuilder;
        this.serverListener = serverListener;
    }
    
    /**
     * 以端口初始化server服务器。
     * 
     * @param port
     */
    public void start()
    {
        try
        {
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(ip, port), 0);
            switch (ioMode)
            {
                case SIMPLE:
                    acceptHandler = new SimpleAcceptHandler(null, serverSocketChannel, serverChannelContextBuilder, serverListener);
                    break;
                case THREAD_ATTACH:
                {
                    ExecutorService executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                        int i = 1;
                        
                        @Override
                        public Thread newThread(Runnable r)
                        {
                            return new Thread(r, "服务端业务处理线程-THREAD_ATTACH-" + (i++));
                        }
                    });
                    acceptHandler = new ThreadAttachAcceptHandler(executorService, serverSocketChannel, serverChannelContextBuilder, serverListener);
                    break;
                }
                case CHANNEL_ATTACH:
                {
                    ExecutorService executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                        int i = 1;
                        
                        @Override
                        public Thread newThread(Runnable r)
                        {
                            return new Thread(r, "服务端业务处理线程-CHANNEL_ATTACH-" + (i++));
                        }
                    });
                    acceptHandler = new ChannelAttachAcceptHandler(executorService, serverSocketChannel, serverChannelContextBuilder, serverListener);
                    break;
                }
                case MUTLI_ATTACH:
                {
                    int size = 1;
                    while (size < businessProcessorNum)
                    {
                        size = size << 1;
                    }
                    businessProcessorNum = size;
                    ExecutorService executorService = Executors.newFixedThreadPool(businessProcessorNum, new ThreadFactory() {
                        int i = 1;
                        
                        @Override
                        public Thread newThread(Runnable r)
                        {
                            return new Thread(r, "服务端业务处理线程-MUTLI_ATTACH-" + (i++));
                        }
                    });
                    acceptHandler = new MutliAttachAcceptHandler(businessProcessorNum, executorService, serverSocketChannel, serverChannelContextBuilder, serverListener);
                    break;
                }
                default:
                    break;
            }
            serverSocketChannel.accept(null, acceptHandler);
        }
        catch (IOException e)
        {
            throw new JustThrowException(e);
        }
    }
    
    public void stop()
    {
        try
        {
            acceptHandler.stop();
            serverSocketChannel.close();
            channelGroup.shutdownNow();
            channelGroup.awaitTermination(10, TimeUnit.SECONDS);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
