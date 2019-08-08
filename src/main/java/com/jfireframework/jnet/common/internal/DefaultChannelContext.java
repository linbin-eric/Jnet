package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.List;

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
    public void write(IoBuffer buffer)
    {
         writeCompletionHandler.process(buffer);
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
        List<DataProcessor> list = new ArrayList<>();
        list.add(readCompletionHandler);
        for (DataProcessor<?> dataProcessor : dataProcessors)
        {
            list.add(dataProcessor);
        }
        list.add(writeCompletionHandler);
        dataProcessors = list.toArray(new DataProcessor[0]);
        for (int i = 0; i + 1 < dataProcessors.length; i++)
        {
            dataProcessors[i].bindDownStream(dataProcessors[i + 1]);
        }
        for (DataProcessor dataProcessor : dataProcessors)
        {
            dataProcessor.bind(this);
        }
    }

    @Override
    public void close()
    {
        close(null);
    }

    @Override
    public void close(Throwable e)
    {
        try
        {
            socketChannel.close();
            aioListener.onClose(socketChannel, e);
        }
        catch (IOException e1)
        {
            ;
        }
    }
}
