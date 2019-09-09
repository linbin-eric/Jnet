package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.AdaptiveReadCompletionHandler;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;
import com.jfireframework.jnet.common.internal.DefaultWriteCompleteHandler;
import com.jfireframework.jnet.common.util.ReflectUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class DefaultClient implements JnetClient
{
    private static final int                       NOT_INIT     = 1;
    private static final int                       CONNECTED    = 2;
    private static final int                       DISCONNECTED = 3;
    private final        String                    ip;
    private final        int                       port;
    private final        AioListener               aioListener;
    private final        BufferAllocator           allocator;
    private final        boolean                   preferBlock  = false;
    private final        AsynchronousChannelGroup  channelGroup;
    private              ChannelContextInitializer channelContextInitializer;
    private              ChannelContext            channelContext;
    private              int                       state        = NOT_INIT;

    public DefaultClient(ChannelContextInitializer channelContextInitializer, String ip, int port, AioListener aioListener, BufferAllocator allocator, AsynchronousChannelGroup channelGroup)
    {
        this.channelContextInitializer = channelContextInitializer;
        this.ip = ip;
        this.port = port;
        this.aioListener = aioListener;
        this.allocator = allocator;
        this.channelGroup = channelGroup;
    }

    @Override
    public void write(IoBuffer packet) throws Exception
    {
        write(packet, preferBlock);
    }

    private void connectIfNecessary()
    {
        if (state == NOT_INIT || state == DISCONNECTED)
        {
            try
            {
                AsynchronousSocketChannel asynchronousSocketChannel = channelGroup == null ? AsynchronousSocketChannel.open() : AsynchronousSocketChannel.open(channelGroup);
                Future<Void>              future                    = asynchronousSocketChannel.connect(new InetSocketAddress(ip, port));
                future.get();
                final ReadCompletionHandler  readCompletionHandler  = new AdaptiveReadCompletionHandler(aioListener, allocator, asynchronousSocketChannel);
                final WriteCompletionHandler writeCompletionHandler = new DefaultWriteCompleteHandler(asynchronousSocketChannel, aioListener, allocator, 1024 * 1024 * 2);
                channelContext = new DefaultChannelContext(asynchronousSocketChannel, aioListener, readCompletionHandler, writeCompletionHandler)
                {
//                    public void setDataProcessor(DataProcessor<?>... dataProcessors)
//                    {
//                        for (int i = 1; i < dataProcessors.length; i++)
//                        {
//                            dataProcessors[i].bindUpStream(dataProcessors[i - 1]);
//                            dataProcessors[i - 1].bindDownStream(dataProcessors[i]);
//                        }
//                        readCompletionHandler.bindDownStream(dataProcessors[0]);
//                        dataProcessors[0].bindUpStream(readCompletionHandler);
//                        dataProcessors[dataProcessors.length - 1].bindDownStream(writeCompletionHandler);
//                        writeCompletionHandler.bindUpStream(dataProcessors[dataProcessors.length - 1]);
//                        for (DataProcessor dataProcessor : dataProcessors)
//                        {
//                            dataProcessor.bind(this);
//                        }
//                    }
                };
                channelContextInitializer.onChannelContextInit(channelContext);
                readCompletionHandler.start();
                state = CONNECTED;
            }
            catch (Exception e)
            {
                ReflectUtil.throwException(e);
                return;
            }
        }
    }

    void blockWrite(IoBuffer buffer)
    {
        ByteBuffer readableByteBuffer = buffer.readableByteBuffer();
        while (readableByteBuffer.hasRemaining())
        {
            try
            {
                channelContext.socketChannel().write(readableByteBuffer).get();
            }
            catch (Throwable e)
            {
                close();
                ReflectUtil.throwException(e);
            }
        }
        buffer.free();
    }

    void nonBlockWrite(IoBuffer buffer)
    {
        channelContext.write(buffer);
    }

    @Override
    public void close()
    {
        if (state == NOT_INIT || state == DISCONNECTED)
        {
            return;
        }
        else
        {
            state = DISCONNECTED;
            try
            {
                channelContext.socketChannel().close();
            }
            catch (IOException e)
            {
                ReflectUtil.throwException(e);
            }
        }
    }

    @Override
    public void write(IoBuffer packet, boolean block) throws Exception
    {
        connectIfNecessary();
        if (block)
        {
            blockWrite(packet);
        }
        else
        {
            nonBlockWrite(packet);
        }
    }

    @Override
    public boolean preferBlock()
    {
        return preferBlock;
    }
}
