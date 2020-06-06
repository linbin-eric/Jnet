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
    private       Consumer                prepareFirstRead;
    private       Consumer                channelClose;
    private       Consumer                exceptionCatch;
    private       Consumer                endOfReadLife;
    private       Consumer                endOfWriteLife;
    private       Operator                readHandler;
    private       Operator                writeHandler;

    public DefaultProcessorContext(JnetWorker jnetWorker, ChannelContext channelContext)
    {
        this.jnetWorker = jnetWorker;
        this.channelContext = channelContext;
    }

    @Override
    public void fireRead(Object data)
    {
        readHandler.accept(read, jnetWorker, channelContext, data);
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHandler.accept(write, jnetWorker, channelContext, data);
    }

    @Override
    public void firePrepareFirstRead()
    {
        readHandler.accept(prepareFirstRead, jnetWorker, channelContext, null);
    }

    @Override
    public void fireChannelClose()
    {
        readHandler.accept(channelClose, jnetWorker, channelContext, null);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHandler.accept(exceptionCatch, jnetWorker, channelContext, e);
    }

    @Override
    public void fireEndOfReadLife()
    {
        readHandler.accept(endOfReadLife, jnetWorker, channelContext, null);
    }

    @Override
    public void fireEndOfWriteLife()
    {
        writeHandler.accept(endOfWriteLife, jnetWorker, channelContext, null);
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
            readHandler = WorkOperator.INSTANCE;
            ReadProcessor readProcessor = (ReadProcessor) processor;
            read = data -> readProcessor.read(data, next);
            prepareFirstRead = data -> readProcessor.prepareFirstRead(next);
            channelClose = data -> readProcessor.channelClose(next);
            exceptionCatch = e -> readProcessor.exceptionCatch((Throwable) e, next);
            endOfReadLife = data -> readProcessor.endOfReadLife(next);
        }
        else
        {
            readHandler = NoOp.INSTANCE;
            read = data -> next.fireRead(data);
            prepareFirstRead = data -> next.firePrepareFirstRead();
            channelClose = data -> next.fireChannelClose();
            exceptionCatch = e -> next.fireExceptionCatch((Throwable) e);
            endOfReadLife = data -> next.fireEndOfReadLife();
        }
        if (processor instanceof WriteProcessor)
        {
            writeHandler = WorkOperator.INSTANCE;
            WriteProcessor writeProcessor = (WriteProcessor) processor;
            write = data -> writeProcessor.write(data, prev);
            endOfWriteLife = data -> writeProcessor.endOfWriteLife(prev);
        }
        else
        {
            writeHandler = NoOp.INSTANCE;
            write = data -> prev.fireWrite(data);
            endOfWriteLife = data -> prev.fireEndOfWriteLife();
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

    @FunctionalInterface
    interface Operator
    {
        void accept(Consumer consumer, JnetWorker jnetWorker, ChannelContext channelContext, Object data);
    }

    static class WorkOperator implements Operator
    {

        public static final WorkOperator INSTANCE = new WorkOperator();

        private WorkOperator()
        {
        }

        @Override
        public void accept(Consumer consumer, JnetWorker jnetWorker, ChannelContext channelContext, Object data)
        {
            if (Thread.currentThread() == jnetWorker.thread())
            {
                try
                {
                    consumer.accept(data);
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
                        consumer.accept(data);
                    }
                    catch (Throwable e)
                    {
                        channelContext.pipeline().fireExceptionCatch(e);
                    }
                });
            }
        }
    }

    static class NoOp implements Operator
    {

        public static final NoOp INSTANCE = new NoOp();

        @Override
        public void accept(Consumer consumer, JnetWorker jnetWorker, ChannelContext channelContext, Object data)
        {
            try
            {
                consumer.accept(data);
            }
            catch (Throwable e)
            {
                channelContext.pipeline().fireExceptionCatch(e);
            }
        }
    }
}
