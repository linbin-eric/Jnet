package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

import javax.xml.crypto.Data;
import java.util.function.Consumer;
import java.util.function.Function;

public class DefaultProcessorContext implements ProcessorContext
{
    private       DefaultProcessorContext next;
    private       DefaultProcessorContext prev;
    private final JnetWorker              jnetWorker;
    private final ChannelContext          channelContext;
    private       Consumer                read;
    private       Consumer                write;
    private       Consumer                prepareFirstRead;
    private       Consumer                channelClose;
    private       Consumer                exceptionCatch;
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
    public void firePrepareFirstRead()
    {
        prepareFirstRead.accept(null);
    }

    @Override
    public void fireChannelClose()
    {
        channelClose.accept(null);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        exceptionCatch.accept(e);
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
            read = new DataOperator(data -> readProcessor.read(data, next));
            prepareFirstRead = new DataOperator(data -> readProcessor.prepareFirstRead(next));
            channelClose = new DataOperator(data -> readProcessor.channelClose(next));
            exceptionCatch = new DataOperator(e -> readProcessor.exceptionCatch((Throwable) e, next));
        }
        else
        {
            read = new NoOp(data -> next.fireRead(data));
            prepareFirstRead = new NoOp(data -> next.firePrepareFirstRead());
            channelClose = new NoOp(data -> next.fireChannelClose());
            exceptionCatch = new NoOp(e -> next.fireExceptionCatch((Throwable) e));
        }
        if (processor instanceof WriteProcessor)
        {
            writeProcessor = (WriteProcessor) processor;
            write = new DataOperator(data -> {writeProcessor.write(data, prev); });
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
