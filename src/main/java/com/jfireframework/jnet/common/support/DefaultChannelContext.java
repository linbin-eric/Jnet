package com.jfireframework.jnet.common.support;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultChannelContext implements ChannelContext
{
    private final WriteHandler              writeHandler;
    private final AsynchronousSocketChannel socketChannel;
    private ProcessorInvoker                invoker;
    
    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, int maxMerge, AioListener aioListener)
    {
        this.socketChannel = socketChannel;
        writeHandler = new DefaultWriteHandler(aioListener, this, maxMerge);
    }
    
    @Override
    public void write(IoBuffer buffer)
    {
        writeHandler.write(buffer);
    }
    
    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void setDataProcessor(DataProcessor<?>... dataProcessors)
    {
        ProcessorInvoker last = new ProcessorInvoker() {
            
            @Override
            public void process(Object data) throws Throwable
            {
                throw new IllegalStateException("这是处理链条的结尾，不应该走到这个步骤");
            }
        };
        ProcessorInvoker prev = last;
        for (int i = dataProcessors.length - 1; i >= 0; i--)
        {
            final ProcessorInvoker next = prev;
            final DataProcessor processor = dataProcessors[i];
            ProcessorInvoker invoker = new ProcessorInvoker() {
                
                @Override
                public void process(Object data) throws Throwable
                {
                    processor.process(data, next);
                }
            };
            prev = invoker;
        }
        invoker = prev;
        for (DataProcessor<?> each : dataProcessors)
        {
            each.initialize(this);
        }
    }
    
    @Override
    public void process(IoBuffer buffer) throws Throwable
    {
        invoker.process(buffer);
    }
    
}
