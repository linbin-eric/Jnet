package com.jfireframework.jnet.common.internal;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class DefaultChannelContext implements ChannelContext
{
    private WriteCompletionHandler          writeCompletionHandler;
    private final AsynchronousSocketChannel socketChannel;
    private ProcessorInvoker                invoker;
    
    public DefaultChannelContext(AsynchronousSocketChannel socketChannel, AioListener aioListener)
    {
        this.socketChannel = socketChannel;
    }
    
    @Override
    public void write(IoBuffer buffer)
    {
        writeCompletionHandler.offer(buffer);
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
            each.bind(this);
        }
    }
    
    @Override
    public void process(IoBuffer buffer) throws Throwable
    {
        invoker.process(buffer);
    }
    
    @Override
    public void bindWriteCompleteHandler(WriteCompletionHandler writeCompletionHandler)
    {
        this.writeCompletionHandler = writeCompletionHandler;
    }
    
}
