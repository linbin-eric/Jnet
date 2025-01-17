package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.util.ChannelConfig;
import lombok.Getter;
import lombok.Setter;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.atomic.AtomicInteger;

public class DefaultPipeline extends AtomicInteger implements InternalPipeline
{
    private static final int                       OPEN  = 1;
    private static final int                       CLOSE = 0;
    private final        AsynchronousSocketChannel socketChannel;
    private final        ChannelConfig             channelConfig;
    private              ReadProcessorNode         readHead;
    private              WriteProcessorNode        writeHead;
    @Setter
    @Getter
    private              Object                    attach;

    public DefaultPipeline(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
        writeHead          = new WriteHead(channelConfig.getWorkerGroup().next());
        readHead           = channelConfig.isREAD_USE_CURRENT_THREAD() ? new ReadHeadUseCurrentThread(this) : new ReadHeadUseWorker(channelConfig.getWorkerGroup().next(), this);
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHead.fireWrite(data);
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        ReadProcessorNode node = readHead;
        while (node.getNext() != null)
        {
            node = node.getNext();
        }
        node.setNext(new ReadProcessorNodeImpl(processor, this));
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor)
    {
        WriteProcessorNode node = writeHead;
        while (node.getNext() != null)
        {
            node = node.getNext();
        }
        node.setNext(new WriteProcessorNodeImpl(processor));
    }

    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }

    @Override
    public ChannelConfig channelConfig()
    {
        return channelConfig;
    }

    @Override
    public void fireRead(Object data)
    {
        readHead.fireRead(data);
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        readHead.fireChannelClose(e);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHead.fireExceptionCatch(e);
    }

    @Override
    public void complete()
    {
        addReadProcessor(TailReadProcessor.INSTANCE);
        addWriteProcessor(new TailWriteProcessor(this));
        readHead.firePipelineComplete(this);
        new AdaptiveReadCompletionHandler(this).start();
    }

    @Override
    public void fireReadClose()
    {
        readHead.fireReadClose();
    }

    @Override
    public void fireWriteClose()
    {
        writeHead.fireWriteClose();
    }

    @Override
    public void close(Throwable e)
    {
        if (!compareAndSet(OPEN, CLOSE))
        {
            return;
        }
        try
        {
            socketChannel.close();
        }
        catch (Throwable ignored)
        {
            ;
        }
    }
}
