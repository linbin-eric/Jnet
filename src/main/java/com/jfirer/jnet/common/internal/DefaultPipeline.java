package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

import java.util.function.Function;

public class DefaultPipeline implements InternalPipeline
{
    protected       JnetWorker         worker;
    protected       ChannelContext     channelContext;
    protected       ReadProcessorNode  readHead;
    protected       WriteProcessorNode writeHead;
    protected final boolean            useVirtualThread;

    public DefaultPipeline(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
        useVirtualThread    = channelContext.channelConfig().isUseVirtualThread();
        if (useVirtualThread)
        {
            ;
        }
        else
        {
            worker = channelContext.channelConfig().getWorkerGroup().next();
        }
    }

    public DefaultPipeline(JnetWorker worker, ChannelContext channelContext)
    {
        this.worker         = worker;
        this.channelContext = channelContext;
        useVirtualThread    = channelContext.channelConfig().isUseVirtualThread();
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHead.fireWrite(data);
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        if (useVirtualThread)
        {
            if (readHead == null)
            {
                readHead = new ReadProcessorNodeImpl(processor, this);
            }
            else
            {
                ReadProcessorNode node = readHead;
                while (node.next() != null)
                {
                    node = node.next();
                }
                node.setNext(new ReadProcessorNodeImpl(processor, this));
            }
        }
        else
        {
            addReadProcessor0(processor, pre -> pre == null ? worker : pre.worker());
        }
    }

    private void addReadProcessor0(ReadProcessor<?> processor, Function<ReadProcessorNode, JnetWorker> function)
    {
        if (readHead == null)
        {
            readHead = new ReadProcessorNodeImpl(function.apply(null), processor, this);
        }
        else
        {
            ReadProcessorNode node = readHead;
            while (node.next() != null)
            {
                node = node.next();
            }
            node.setNext(new ReadProcessorNodeImpl(function.apply(node), processor, this));
        }
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor, WorkerGroup group)
    {
        if (useVirtualThread)
        {
            throw new UnsupportedOperationException("采用虚拟线程，不支持自定义 worker");
        }
        else
        {
            addReadProcessor0(processor, pre -> group.next());
        }
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor)
    {
        if (useVirtualThread)
        {
            if (writeHead == null)
            {
                writeHead = new WriteProcessorNodeImpl(processor);
            }
            else
            {
                WriteProcessorNode node = writeHead;
                while (node.next() != null)
                {
                    node = node.next();
                }
                node.setNext(new WriteProcessorNodeImpl(processor));
            }
        }
        else
        {
            addWriteProcessor0(processor, pre -> pre == null ? worker : pre.worker());
        }
    }

    private void addWriteProcessor0(WriteProcessor<?> processor, Function<WriteProcessorNode, JnetWorker> function)
    {
        if (writeHead == null)
        {
            writeHead = new WriteProcessorNodeImpl(function.apply(null), processor);
        }
        else
        {
            WriteProcessorNode node = writeHead;
            while (node.next() != null)
            {
                node = node.next();
            }
            node.setNext(new WriteProcessorNodeImpl(function.apply(node), processor));
        }
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor, WorkerGroup group)
    {
        if (useVirtualThread)
        {
            throw new UnsupportedOperationException("采用虚拟线程，不支持自定义 worker");
        }
        else
        {
            addWriteProcessor0(processor, pre -> group.next());
        }
    }

    @Override
    public ChannelContext channelContext()
    {
        return channelContext;
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
}
