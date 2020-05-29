package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

import java.util.function.Consumer;

public class DefaultProcessorContext implements ProcessorContext
{
    private       DefaultProcessorContext next;
    private       DefaultProcessorContext prev;
    private final JnetWorker              jnetWorker;
    private final ChannelContext          channelContext;
    private       Consumer                read;
    private       Consumer                write;
    private       ReadProcessor           readProcessor;
    private       WriteProcessor          writeProcessor;

    public DefaultProcessorContext(JnetWorker jnetWorker, ChannelContext channelContext, Pipeline pipeline)
    {
        this.jnetWorker = jnetWorker;
        this.channelContext = channelContext;
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
    public ChannelContext channelContext()
    {
        return channelContext;
    }

    public void setProcessor(Object processor)
    {
        if (processor instanceof ReadProcessor)
        {
            readProcessor = (ReadProcessor) processor;
            read = (data) -> {
                if (Thread.currentThread() == jnetWorker.thread())
                {
                    try
                    {
                        invokeRead(data);
                    }
                    catch (Throwable e)
                    {
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
                            channelContext.close(e);
                        }
                    });
                }
            };
        }
        else
        {
            read = data -> {
                try
                {
                    next.fireRead(data);
                }
                catch (Throwable e)
                {
                    channelContext.close(e);
                }
            };
        }
        if (processor instanceof WriteProcessor)
        {
            writeProcessor = (WriteProcessor) processor;
            write = data -> {
                if (Thread.currentThread() == jnetWorker.thread())
                {
                    try
                    {
                        invokeWrite(data);
                    }
                    catch (Throwable e)
                    {
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
                            channelContext.close(e);
                        }
                    });
                }
            };
        }
        else
        {
            write = data -> {
                try
                {
                    prev.fireWrite(data);
                }
                catch (Throwable e)
                {
                    channelContext.close(e);
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

    private void invokeRead(Object data)
    {
        readProcessor.read(data, next);
    }

    private void invokeWrite(Object data)
    {
        writeProcessor.write(data, prev);
    }

    public JnetWorker worker()
    {
        return jnetWorker;
    }

    public DefaultProcessorContext getPrev()
    {
        return prev;
    }
}
