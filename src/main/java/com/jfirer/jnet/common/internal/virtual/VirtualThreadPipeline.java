package com.jfirer.jnet.common.internal.virtual;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.internal.TailWriteProcessorImpl;

public class VirtualThreadPipeline implements InternalPipeline
{
    private ChannelContext     channelContext;
    private ReadProcessorNode  readHead;
    private WriteProcessorNode writeHead;

    public VirtualThreadPipeline(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }

    @Override
    public void fireRead(Object data)
    {
        readHead.fireRead(data);
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        readHead.fireChannelClose(e);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHead.fireExceptionCatch(e);
    }

    @Override
    public void complete()
    {
        addReadProcessor(ReadProcessor.TAIL);
        addWriteProcessor(new TailWriteProcessorImpl());
        readHead.firePipelineComplete();
        writeHead.firePipelineComplete(channelContext);
    }

    @Override
    public void fireReadClose()
    {
        readHead.fireReadClose();
    }

    @Override
    public void fireWriteClose()
    {
        writeHead.fireWriteClose();
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHead.fireWrite(data);
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        if (readHead == null)
        {
            readHead = new VirtualReadProcessorNode(processor, this);
        }
        else
        {
            ReadProcessorNode node = readHead;
            while (node.next() != null)
            {
                node = node.next();
            }
            node.setNext(new VirtualReadProcessorNode(processor, this));
        }
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor, WorkerGroup group)
    {
        throw new UnsupportedOperationException("采用虚拟线程，不支持自定义 worker");
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor)
    {
        if (writeHead == null)
        {
            writeHead = new VirtualWriteProcessorNode(processor);
        }
        else
        {
            WriteProcessorNode node = writeHead;
            while (node.next() != null)
            {
                node = node.next();
            }
            node.setNext(new VirtualWriteProcessorNode(processor));
        }
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor, WorkerGroup group)
    {
        throw new UnsupportedOperationException("采用虚拟线程，不支持自定义 worker");
    }

    @Override
    public ChannelContext channelContext()
    {
        return channelContext;
    }
}
