package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.AdaptiveReadCompletionHandler;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultWriteCompleteHandler;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.common.util.ReflectUtil;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;

public class DefaultClient implements JnetClient
{
    private static final int                       NOT_INIT     = 1;
    private static final int                       CONNECTED    = 2;
    private static final int                       DISCONNECTED = 3;
    private final        boolean                   preferBlock  = false;
    private              ChannelContextInitializer initializer;
    private              ChannelConfig             channelConfig;
    private              ChannelContext            channelContext;
    private              int                       state        = NOT_INIT;

    public DefaultClient(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        this.channelConfig = channelConfig;
        this.initializer = initializer;
    }

    @Override
    public void write(IoBuffer packet) throws Exception
    {
        write(packet, preferBlock);
    }

    public void connectIfNecessary()
    {
        if (state == NOT_INIT)
        {
            try
            {
                AsynchronousSocketChannel asynchronousSocketChannel = AsynchronousSocketChannel.open(channelConfig.getChannelGroup());
                Future<Void>              future                    = asynchronousSocketChannel.connect(new InetSocketAddress(channelConfig.getIp(), channelConfig.getPort()));
                future.get();
                final ReadCompletionHandler  readCompletionHandler  = new AdaptiveReadCompletionHandler(channelConfig, asynchronousSocketChannel);
                final WriteCompletionHandler writeCompletionHandler = new DefaultWriteCompleteHandler(channelConfig, asynchronousSocketChannel);
                channelContext = new DefaultChannelContext(asynchronousSocketChannel, channelConfig.getAioListener(), readCompletionHandler, writeCompletionHandler);
                initializer.onChannelContextInit(channelContext);
                readCompletionHandler.start();
                state = CONNECTED;
            }
            catch (Exception e)
            {
                ReflectUtil.throwException(e);
                return;
            }
        }
        else if (state == DISCONNECTED)
        {
            throw new IllegalStateException("客户端已经关闭");
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
