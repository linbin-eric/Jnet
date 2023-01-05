package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

import java.util.function.Function;
import java.util.function.Supplier;

public class DefaultPipeline implements InternalPipeline
{
    private JnetWorker         worker;
    private ChannelContext     channelContext;
    private ReadProcessorNode  readHead;
    private WriteProcessorNode writeHead;

    public DefaultPipeline(JnetWorker worker, ChannelContext channelContext)
    {
        this.worker = worker;
        this.channelContext = channelContext;
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHead.fireWrite(data);
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        addReadProcessor0(processor, () -> worker, node -> node.worker());
    }

    private void addReadProcessor0(ReadProcessor<?> processor, Supplier<JnetWorker> supplier, Function<ReadProcessorNode, JnetWorker> function)
    {
        if (readHead == null)
        {
            readHead = new ReadProcessorNodeImpl(supplier.get(), processor);
        }
        else
        {
            ReadProcessorNode node = readHead;
            while (node.next() != null)
            {
                node = node.next();
            }
            node.setNext(new ReadProcessorNodeImpl(function.apply(node), processor));
        }
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor, WorkerGroup group)
    {
        addReadProcessor0(processor, () -> group.next(), node -> group.next());
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor)
    {
        addWriteProcessor0(processor, () -> worker, node -> node.worker());
    }

    private void addWriteProcessor0(WriteProcessor<?> processor, Supplier<JnetWorker> supplier, Function<WriteProcessorNode, JnetWorker> function)
    {
        if (writeHead == null)
        {
            writeHead = new WriteProcessorNodeImpl(supplier.get(), processor);
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
        addWriteProcessor0(processor, () -> group.next(), node -> group.next());
    }

    @Override
    public void fireRead(Object data)
    {
        readHead.fireRead(data);
    }

    @Override
    public void fireChannelClose()
    {
        readHead.fireChannelClose();
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHead.fireExceptionCatch(e);
    }

    @Override
    public void complete()
    {
        addReadProcessor0(ReadProcessor.NONE_OP, () -> worker, node -> node.worker());
        addWriteProcessor0(new DefaultWriteCompleteHandler(channelContext), () -> worker, node -> node.worker());
        readHead.firePipelineComplete();
        writeHead.firePipelineComplete();
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
