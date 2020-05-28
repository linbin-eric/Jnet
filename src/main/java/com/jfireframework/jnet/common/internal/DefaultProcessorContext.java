package com.jfireframework.jnet.common.internal;

import com.jfireframework.jnet.common.api.*;

import java.util.function.Consumer;

public class DefaultProcessorContext implements ProcessorContext
{
    private       DefaultProcessorContext next;
    private       DefaultProcessorContext prev;
    private final Thread                  thread;
    private final JnetWorker              jnetWorker;
    private final ChannelContext          channelContext;
    private       Consumer                read;
    private       Consumer                write;
    private       Pipeline                pipeline;
    private       ReadProcessor           readProcessor;
    private       WriteProcessor          writeProcessor;

    public DefaultProcessorContext(JnetWorker jnetWorker, ChannelContext channelContext, Pipeline pipeline)
    {
        this.thread = jnetWorker.thread();
        this.jnetWorker = jnetWorker;
        this.channelContext = channelContext;
        this.pipeline = pipeline;
    }

    @Override
    public void fireRead(Object data)
    {
        read.accept(data);
    }

    @Override
    public void fireWrite(Object data)
    {
        write.accept(data);
    }

    @Override
    public JnetWorker worker()
    {
        return jnetWorker;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }

    public void setProcessor(Object processor)
    {
        if (processor instanceof ReadProcessor)
        {
            readProcessor = (ReadProcessor) processor;
            read = (data) -> {
                if (Thread.currentThread() == worker().thread())
                {
                    try
                    {
                        invokeRead(data);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                        channelContext.close(e);
                    }
                }
                else
                {
                    jnetWorker.submit(() -> {
                        try
                        {
                            invokeRead(data);
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                            channelContext.close(e);
                        }
                    });
                }
            };
        }
        else
        {
            read = data -> {
                try{

                next.fireRead(data);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            };
        }
        if (processor instanceof WriteProcessor)
        {
            writeProcessor = (WriteProcessor) processor;
            write = data -> {
                if (Thread.currentThread() == worker().thread())
                {
                    try
                    {
                        invokeWrite(data);
                    }
                    catch (Throwable e)
                    {
                        e.printStackTrace();
                        channelContext.close(e);
                    }
                }
                else
                {
                    jnetWorker.submit(() -> {
                        try
                        {
                            invokeWrite(data);
                        }
                        catch (Throwable e)
                        {
                            e.printStackTrace();
                            channelContext.close(e);
                        }
                    });
                }
            };
        }
        else
        {
            write = data -> {
                try{

                prev.fireWrite(data);
                }catch (Throwable e){
                    e.printStackTrace();
                }
            };
        }
    }

    public void setNext(ProcessorContext next)
    {
        this.next = (DefaultProcessorContext) next;
    }

    public void setPrev(ProcessorContext prev)
    {
        this.prev = (DefaultProcessorContext) prev;
    }

    public void invokeRead(Object data)
    {
        readProcessor.read(data, next);
    }

    public void invokeWrite(Object data)
    {
        writeProcessor.write(data, prev);
    }
}
