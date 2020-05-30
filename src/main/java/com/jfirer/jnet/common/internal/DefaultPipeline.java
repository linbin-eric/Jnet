package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

public class DefaultPipeline implements Pipeline
{

    private WorkerGroup             group;
    private ChannelContext          channelContext;
    private DefaultProcessorContext head;
    private DefaultProcessorContext tail;

    public DefaultPipeline(WorkerGroup group, ChannelContext channelContext)
    {
        this.group = group;
        this.channelContext = channelContext;
    }

    @Override
    public void add(Object processor)
    {
        if (tail == null)
        {
            setHead(processor, group.next());
        }
        else
        {
            setTail(processor, tail.worker());
        }
    }

    private void setTail(Object processor, JnetWorker worker)
    {
        DefaultProcessorContext prev = tail.getPrev();
        DefaultProcessorContext ctx  = new DefaultProcessorContext(worker, channelContext);
        ctx.setProcessor(processor);
        prev.setNext(ctx);
        if (worker != tail.worker())
        {
            newTail(worker);
        }
        ctx.setPrev(prev);
        ctx.setNext(tail);
        tail.setPrev(ctx);
    }

    class TailProcessor implements ReadProcessor, WriteProcessor
    {
        @Override
        public void read(Object data, ProcessorContext ctx)
        {
        }

        @Override
        public void prepareFirstRead(ProcessorContext ctx)
        {
        }

        @Override
        public void channelClose(ProcessorContext ctx)
        {
        }

        @Override
        public void exceptionCatch(Throwable e, ProcessorContext ctx)
        {
            channelContext.close(e);
        }

        @Override
        public void write(Object data, ProcessorContext ctx)
        {
            ctx.fireWrite(data);
        }

        @Override
        public void endOfLife(ProcessorContext next)
        {
        }
    }

    private void newTail(JnetWorker worker)
    {
        tail = new DefaultProcessorContext(worker, channelContext);
        tail.setProcessor(new TailProcessor());
    }

    private void setHead(Object processor, JnetWorker jnetWorker)
    {
        head = new DefaultProcessorContext(jnetWorker, channelContext);
        head.setProcessor(new DefaultWriteCompleteHandler(channelContext));
        DefaultProcessorContext second = new DefaultProcessorContext(jnetWorker, channelContext);
        second.setProcessor(processor);
        head.setNext(second);
        second.setPrev(head);
        newTail(jnetWorker);
        second.setNext(tail);
        tail.setPrev(second);
    }

    @Override
    public void add(Object processor, WorkerGroup workerGroup)
    {
        if (tail == null)
        {
            setHead(processor, workerGroup.next());
        }
        else
        {
            setTail(processor, workerGroup.next());
        }
    }

    @Override
    public void fireRead(Object data)
    {
        head.fireRead(data);
    }

    @Override
    public void fireWrite(Object data)
    {
        tail.fireWrite(data);
    }

    @Override
    public void fireChannelClose()
    {
        head.fireChannelClose();
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        head.fireExceptionCatch(e);
    }

    @Override
    public void firePrepareFirstRead()
    {
        head.firePrepareFirstRead();
    }

    @Override
    public void fireEndOfLife()
    {
        head.fireEndOfLife();
    }

    public void setChannelContext(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }
}
