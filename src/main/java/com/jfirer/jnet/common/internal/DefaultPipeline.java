package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.util.ChannelConfig;
import lombok.Getter;
import lombok.Setter;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.function.Consumer;

public class DefaultPipeline implements InternalPipeline
{
    private final AsynchronousSocketChannel     socketChannel;
    private final ChannelConfig                 channelConfig;
    private final Consumer<Throwable>           jvmExistHandler;
    private       ReadProcessorNode             readHead;
    private       WriteProcessorNode            writeHead;
    private       AdaptiveReadCompletionHandler adaptiveReadCompletionHandler;
    private       DefaultWriteCompleteHandler   writeCompleteHandler;
    @Setter
    @Getter
    private       RegisterReadListener          registerReadListener = RegisterReadListener.INSTANCE;
    @Setter
    @Getter
    private       WriteListener                 writeListener        = WriteListener.INSTANCE;
    @Setter
    @Getter
    private       Object                        attach;

    public DefaultPipeline(AsynchronousSocketChannel socketChannel, ChannelConfig channelConfig)
    {
        this.socketChannel = socketChannel;
        this.channelConfig = channelConfig;
        jvmExistHandler    = channelConfig.getJvmExistHandler();
        writeHead          = new WriteHead(channelConfig.getWorkerGroup().next());
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        if (readHead == null)
        {
            readHead = new ReadProcessorNodeImpl(processor, this);
            return;
        }
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
    public void fireWrite(Object data)
    {
        try
        {
            writeHead.fireWrite(data);
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(127);
        }
    }

    @Override
    public void fireRead(Object data)
    {
        try
        {
            readHead.fireRead(data);
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(127);
        }
    }

    @Override
    public void complete()
    {
        adaptiveReadCompletionHandler = new AdaptiveReadCompletionHandler(this);
        addReadProcessor(TailReadProcessor.INSTANCE);
        writeCompleteHandler = new DefaultWriteCompleteHandler(this);
        addWriteProcessor(new TailWriteProcessor(writeCompleteHandler));
        try
        {
            readHead.firePipelineComplete(this);
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(127);
        }
        adaptiveReadCompletionHandler.setRegisterReadListener(registerReadListener);
        writeCompleteHandler.setWriteListener(writeListener);
        adaptiveReadCompletionHandler.start();
    }

    @Override
    public void fireReadFailed(Throwable e)
    {
        try
        {
            readHead.fireReadFailed(e);
        }
        catch (Throwable e1)
        {
            jvmExistHandler.accept(e1);
            System.exit(127);
        }
    }

    @Override
    public void fireWriteFailed(Throwable e)
    {
        try
        {
            writeHead.fireWriteFailed(e);
        }
        catch (Throwable e1)
        {
            jvmExistHandler.accept(e1);
            System.exit(127);
        }
    }

    @Override
    public void fireChannelClosed()
    {
        try
        {
            writeHead.fireChannelClosed();
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(127);
        }
    }
}
