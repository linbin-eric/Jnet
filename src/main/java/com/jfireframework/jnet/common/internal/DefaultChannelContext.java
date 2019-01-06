package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.processor.BackPressureHelper;

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
    public boolean availableForWrite()
    {
        return writeCompletionHandler.canAccept();
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
        /**检查是否正确放置了BackPressureHelper**/
        if (writeCompletionHandler.isBoundary() == false)
        {
            return;
        }
        for (int i = 1; i < dataProcessors.length; i++)
        {
            if (dataProcessors[i].isBoundary())
            {
                if (dataProcessors[i - 1] instanceof BackPressureHelper == false)
                {
                    throw new IllegalArgumentException("背压模式开启下，在" + dataProcessors[i].getClass().getName() + "之前需要放置BackPressureHelper");
                }
            }
        }
        if (dataProcessors[dataProcessors.length - 1] instanceof BackPressureHelper == false)
        {
            throw new IllegalArgumentException("开启背压模式下，最后一个处理器必须是BackPressureHelper");
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
        } catch (IOException e1)
        {
            ;
        }
    }
}
