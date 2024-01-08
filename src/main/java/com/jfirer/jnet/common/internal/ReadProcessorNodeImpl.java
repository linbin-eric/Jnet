package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

public class ReadProcessorNodeImpl implements ReadProcessorNode
{
    private final JnetWorker        worker;
    private final ReadProcessor     processor;
    private final Pipeline          pipeline;
    private final boolean           useCurrentThread;
    private       ReadProcessorNode next;

    public ReadProcessorNodeImpl(ReadProcessor processor, Pipeline pipeline)
    {
        this.processor        = processor;
        this.pipeline         = pipeline;
        this.worker           = null;
        this.useCurrentThread = true;
    }

    public ReadProcessorNodeImpl(JnetWorker worker, ReadProcessor processor, Pipeline pipeline)
    {
        this.worker           = worker;
        this.processor        = processor;
        this.pipeline         = pipeline;
        this.useCurrentThread = false;
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
    public void fireRead(Object data)
    {
        if (useCurrentThread)
        {
            processor.read(data, next);
        }
        else
        {
            doWork(() -> processor.read(data, next));
        }
    }

    @Override
    public void firePipelineComplete()
    {
        if (useCurrentThread)
        {
            processor.pipelineComplete(next);
        }
        else
        {
            doWork(() -> processor.pipelineComplete(next));
        }
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        if (useCurrentThread)
        {
            processor.exceptionCatch(e, next);
        }
        else
        {
            doWork(() -> processor.exceptionCatch(e, next));
        }
    }

    @Override
    public void fireReadClose()
    {
        if (useCurrentThread)
        {
            processor.readClose(next);
        }
        else
        {
            doWork(() -> processor.readClose(next));
        }
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        if (useCurrentThread)
        {
            processor.channelClose(next, e);
        }
        else
        {
            doWork(() -> processor.channelClose(next, e));
        }
    }

    @Override
    public void setNext(ReadProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public ReadProcessorNode next()
    {
        return next;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }

    @Override
    public JnetWorker worker()
    {
        return worker;
    }
}
