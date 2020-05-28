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
        DefaultProcessorContext prev = tail;
        DefaultProcessorContext ctx  = new DefaultProcessorContext(worker, channelContext, this);
        ctx.setProcessor(processor);
        prev.setNext(ctx);
        ctx.setPrev(prev);
        tail = ctx;
    }

    private void setHead(Object processor, JnetWorker jnetWorker)
    {
        head = new DefaultProcessorContext(jnetWorker, channelContext, this);
        head.setProcessor(new DefaultWriteCompleteHandler(channelContext,jnetWorker.thread()));
        tail = new DefaultProcessorContext(jnetWorker, channelContext, this);
        tail.setProcessor(processor);
        head.setNext(tail);
        tail.setPrev(head);
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

    public void buildPipeline()
    {
        DefaultProcessorContext last = new DefaultProcessorContext(tail.worker(), channelContext, this);
        last.setProcessor((ReadProcessor) (data, ctx) -> {
        });
        tail.setNext(last);
        last.setPrev(tail);
        tail = last;
    }

    @Override
    public void read(Object data)
    {
        head.fireRead(data);
    }

    @Override
    public void write(Object data)
    {
        tail.fireWrite(data);
    }
}
