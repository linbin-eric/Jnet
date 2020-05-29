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
            read = new DataOperator(data -> {
                invokeRead(data);
            });
        }
        else
        {
            read = new NoOp(data -> next.fireRead(data));
        }
        if (processor instanceof WriteProcessor)
        {
            writeProcessor = (WriteProcessor) processor;
            write = new DataOperator(data -> {
                invokeWrite(data);
            });
        }
        else
        {
            write = new NoOp(data -> prev.fireWrite(data));
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

    class DataOperator implements Consumer<Object>
    {
        Consumer<Object> handler;

        DataOperator(Consumer<Object> handler)
        {
            this.handler = handler;
        }

        @Override
        public void accept(Object o)
        {
            if (Thread.currentThread() == jnetWorker.thread())
            {
                try
                {
                    handler.accept(o);
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
                        handler.accept(o);
                    }
                    catch (Throwable e)
                    {
                        channelContext.close(e);
                    }
                });
            }
        }
    }

    class NoOp implements Consumer<Object>
    {
        Consumer handler;

        public NoOp(Consumer handler)
        {
            this.handler = handler;
        }

        @Override
        public void accept(Object o)
        {
            try
            {
                handler.accept(o);
            }
            catch (Throwable e)
            {
                channelContext.close(e);
            }
        }
    }
}
