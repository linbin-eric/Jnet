package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.util.ChannelConfig;
import lombok.Getter;
import lombok.Setter;

import java.nio.channels.AsynchronousSocketChannel;

public class DefaultPipeline implements InternalPipeline
{
    private final AsynchronousSocketChannel     socketChannel;
    private final ChannelConfig                 channelConfig;
    private       ReadProcessorNode             readHead;
    private       WriteProcessorNode            writeHead;
    private AdaptiveReadCompletionHandler adaptiveReadCompletionHandler;
    private DefaultWriteCompleteHandler   writeCompleteHandler;
    @Setter
    @Getter
    private RegisterReadCallback          registerReadCallback    = RegisterReadCallback.INSTANCE;
    @Setter
    @Getter
    private       PartWriteFinishCallback       partWriteFinishCallback = PartWriteFinishCallback.INSTANCE;
    @Setter
    @Getter
    private       Object                        attach;

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
    public void shutdownInput()
    {
        try
        {
            socketChannel.shutdownInput();
        }
        catch (Throwable ex)
        {
            ;
        }
        writeCompleteHandler.noticeClose();
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
    public long writeQueueCapacity()
    {
        return writeCompleteHandler.getQueueCapacity().get();
    }

    @Override
    public void fireRead(Object data)
    {
        readHead.fireRead(data);
    }

    @Override
    public void complete()
    {
        adaptiveReadCompletionHandler = new AdaptiveReadCompletionHandler(this);
        addReadProcessor(TailReadProcessor.INSTANCE);
        writeCompleteHandler = new DefaultWriteCompleteHandler(this);
        addWriteProcessor(new TailWriteProcessor(writeCompleteHandler));
        readHead.firePipelineComplete(this);
        adaptiveReadCompletionHandler.setRegisterReadCallback(registerReadCallback);
        writeCompleteHandler.setPartWriteFinishCallback(partWriteFinishCallback);
        adaptiveReadCompletionHandler.start();
    }

    @Override
    public void fireReadFailed(Throwable e)
    {
        readHead.fireReadFailed(e);
    }

    @Override
    public void fireWriteFailed(Throwable e)
    {
        writeHead.fireWriteFailed(e);
    }

    @Override
    public void fireChannelClosed()
    {
        writeHead.fireChannelClosed();
    }
}
