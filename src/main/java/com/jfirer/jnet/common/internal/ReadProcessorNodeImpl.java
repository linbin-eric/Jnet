package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

public class ReadProcessorNodeImpl implements ReadProcessorNode
{
    private JnetWorker        worker;
    private ReadProcessor     processor;
    private ReadProcessorNode next;

    public ReadProcessorNodeImpl(JnetWorker worker, ReadProcessor processor)
    {
        this.worker = worker;
        this.processor = processor;
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
        doWork(() -> processor.read(data, next));
    }

    @Override
    public void firePipelineComplete()
    {
        doWork(() -> processor.pipelineComplete(next));
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        doWork(() -> processor.exceptionCatch(e, next));
    }

    @Override
    public void fireReadClose()
    {
        doWork(() -> processor.readClose(next));
    }

    @Override
    public void fireChannelClose()
    {
        doWork(() -> processor.channelClose(next));
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
    public JnetWorker worker()
    {
        return worker;
    }
}
