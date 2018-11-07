package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;

public class DefaultChannelContext implements ChannelContext
{
    private WriteCompletionHandler    writeCompletionHandler;
    private ReadCompletionHandler     readCompletionHandler;
    private AsynchronousSocketChannel socketChannel;
    private AioListener               aioListener;

    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, AioListener aioListener, ReadCompletionHandler readCompletionHandler, WriteCompletionHandler writeCompletionHandler)
    {
        this.socketChannel = socketChannel;
        this.aioListener = aioListener;
        this.readCompletionHandler = readCompletionHandler;
        this.writeCompletionHandler = writeCompletionHandler;
    }

    @Override
    public boolean writeIfAvailable(IoBuffer buffer)
    {
        return writeCompletionHandler.process(buffer);
    }

    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void setDataProcessor(DataProcessor<?>... dataProcessors)
    {
        for (int i = 1; i < dataProcessors.length; i++)
        {
            dataProcessors[i].bindUpStream(dataProcessors[i - 1]);
            dataProcessors[i - 1].bindDownStream(dataProcessors[i]);
        }
        readCompletionHandler.bindDownStream(dataProcessors[0]);
        dataProcessors[0].bindUpStream(readCompletionHandler);
        dataProcessors[dataProcessors.length - 1].bindDownStream(writeCompletionHandler);
        writeCompletionHandler.bindUpStream(dataProcessors[dataProcessors.length - 1]);
        for (DataProcessor dataProcessor : dataProcessors)
        {
            dataProcessor.bind(this);
        }
    }

    @Override
    public void close()
    {
        try
        {
            socketChannel.close();
            aioListener.onClose(socketChannel, null);
        } catch (IOException e)
        {
            ;
        }
    }

    @Override
    public void close(Throwable e)
    {
        try
        {
            socketChannel.close();
            aioListener.onClose(socketChannel, e);
        } catch (IOException e1)
        {
            ;
        }
    }
}
