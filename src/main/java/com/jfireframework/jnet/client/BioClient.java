package com.jfireframework.jnet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.UnPooledUnRecycledBufferAllocator;
import com.jfireframework.jnet.common.internal.DefaultAioListener;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;

public class BioClient implements AioClient
{
    private String                    ip;
    private int                       port;
    private AsynchronousSocketChannel socketChannel;
    
    @Override
    public void write(IoBuffer packet) throws Exception
    {
        ByteBuffer byteBuffer = packet.readableByteBuffer();
        while (byteBuffer.hasRemaining())
        {
            socketChannel.write(byteBuffer).get();
        }
    }
    
    public BioClient(String ip, int port, ChannelContextInitializer channelContextInitializer)
    {
        this.ip = ip;
        this.port = port;
        AioListener aioListener = new DefaultAioListener();
        try
        {
            socketChannel = AsynchronousSocketChannel.open();
            socketChannel.connect(new InetSocketAddress(ip, port)).get();
            ChannelContext channelContext = new DefaultChannelContext(socketChannel, aioListener);
            ReadCompletionHandler readCompletionHandler = new DefaultReadCompletionHandler(aioListener, UnPooledUnRecycledBufferAllocator.DEFAULT, socketChannel);
            readCompletionHandler.bind(channelContext);
            readCompletionHandler.start();
            channelContextInitializer.onChannelContextInit(channelContext);
        }
        catch (Throwable e)
        {
            e.printStackTrace();
            ReflectUtil.throwException(e);
        }
    }
    
    @Override
    public void close()
    {
        try
        {
            socketChannel.close();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
}
