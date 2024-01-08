package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;

public class WriteProcessorNodeImpl implements WriteProcessorNode
{
    private       JnetWorker         worker;
    private       WriteProcessor     processor;
    private       WriteProcessorNode next;
    private final boolean            onlyUseCurrentThread;

    public WriteProcessorNodeImpl(WriteProcessor processor)
    {
        this.processor       = processor;
        onlyUseCurrentThread = true;
    }

    public WriteProcessorNodeImpl(JnetWorker worker, WriteProcessor processor)
    {
        this.worker          = worker;
        this.processor       = processor;
        onlyUseCurrentThread = false;
    }

    private void doWork(Runnable runnable)
    {
        if (Thread.currentThread() == worker.thread())
        {
            runnable.run();
        }
        else
        {
            worker.submit(runnable);
        }
    }

    @Override
    public void fireWrite(Object data)
    {
        if (onlyUseCurrentThread)
        {
            processor.write(data, next);
        }
        else
        {
            doWork(() -> processor.write(data, next));
        }
    }

    @Override
    public void firePipelineComplete(ChannelContext channelContext)
    {
        if (onlyUseCurrentThread)
        {
            processor.pipelineComplete(next, channelContext);
        }
        else
        {
            doWork(() -> processor.pipelineComplete(next, channelContext));
        }
    }

    @Override
    public void fireWriteClose()
    {
        if (onlyUseCurrentThread)
        {
            processor.writeClose(next);
        }
        else
        {
            doWork(() -> processor.writeClose(next));
        }
    }

    @Override
    public void setNext(WriteProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public WriteProcessorNode next()
    {
        return next;
    }

    @Override
    public JnetWorker worker()
    {
        return worker;
    }
}
