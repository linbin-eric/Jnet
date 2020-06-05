package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class DefaultProcessorContext implements ProcessorContext
{
    private       DefaultProcessorContext      next;
    private       DefaultProcessorContext      prev;
    private final JnetWorker                   jnetWorker;
    private final ChannelContext               channelContext;
    private       Consumer                     read;
    private       Consumer                     write;
    private       Consumer                     prepareFirstRead;
    private       Consumer                     channelClose;
    private       Consumer                     exceptionCatch;
    private       Consumer                     endOfReadLife;
    private       Consumer                     endOfWriteLife;
    private       ReadProcessor                readProcessor;
    private       BiConsumer<Object, Consumer> readHandler;
    private       WriteProcessor               writeProcessor;
    private       BiConsumer<Object, Consumer> writeHandler;

    public DefaultProcessorContext(JnetWorker jnetWorker, ChannelContext channelContext)
    {
        this.jnetWorker = jnetWorker;
        this.channelContext = channelContext;
    }

    @Override
    public void fireRead(Object data)
    {
        readHandler.accept(data, read);
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHandler.accept(data, write);
    }

    @Override
    public void firePrepareFirstRead()
    {
        readHandler.accept(null,prepareFirstRead);
    }

    @Override
    public void fireChannelClose()
    {
        readHandler.accept(null,channelClose);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHandler.accept(e,exceptionCatch);
    }

    @Override
    public void fireEndOfReadLife()
    {
        readHandler.accept(null,endOfReadLife);
    }

    @Override
    public void fireEndOfWriteLife()
    {
        writeHandler.accept(null,endOfWriteLife);
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
            readHandler = new WorkOperator();
            read = data -> readProcessor.read(data, next);
            prepareFirstRead = data -> readProcessor.prepareFirstRead(next);
            channelClose = data -> readProcessor.channelClose(next);
            exceptionCatch = e -> readProcessor.exceptionCatch((Throwable) e, next);
            endOfReadLife = data -> readProcessor.endOfReadLife(next);
        }
        else
        {
            readHandler = new NoOp();
            read = data -> next.fireRead(data);
            prepareFirstRead = data -> next.firePrepareFirstRead();
            channelClose = data -> next.fireChannelClose();
            exceptionCatch = e -> next.fireExceptionCatch((Throwable) e);
            endOfReadLife = data -> next.fireEndOfReadLife();
        }
        if (processor instanceof WriteProcessor)
        {
            writeProcessor = (WriteProcessor) processor;
            writeHandler = new WorkOperator();
            write = data -> writeProcessor.write(data, prev);
            endOfWriteLife = data->writeProcessor.endOfWriteLife(prev);
        }
        else
        {
            writeHandler = new NoOp();
            write = data -> prev.fireWrite(data);
            endOfWriteLife = data->prev.fireEndOfWriteLife();
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

    class WorkOperator implements BiConsumer<Object, Consumer>
    {

        @Override
        public void accept(Object o, Consumer handler)
        {
            if (Thread.currentThread() == jnetWorker.thread())
            {
                try
                {
                    handler.accept(o);
                }
                catch (Throwable e)
                {
                    channelContext.pipeline().fireExceptionCatch(e);
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
                        channelContext.pipeline().fireExceptionCatch(e);
                    }
                });
            }
        }
    }

    class NoOp implements BiConsumer<Object, Consumer>
    {

        @Override
        public void accept(Object o, Consumer handler)
        {
            try
            {
                handler.accept(o);
            }
            catch (Throwable e)
            {
                channelContext.pipeline().fireExceptionCatch(e);
            }
        }
    }
}
