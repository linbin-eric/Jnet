package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import lombok.Data;

@Data
class ReadHeadUseWorker implements ReadProcessorNode
{
    protected final JnetWorker        worker;
    protected final Pipeline          pipeline;
    private         ReadProcessorNode next;

    ReadHeadUseWorker(JnetWorker worker, Pipeline pipeline)
    {
        this.worker   = worker;
        this.pipeline = pipeline;
    }

    @Override
    public void fireRead(Object data)
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireRead(data);
        }
        else
        {
            worker.submit(() -> next.fireRead(data));
        }
    }

    @Override
    public void firePipelineComplete(Pipeline pipeline)
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.firePipelineComplete(pipeline);
        }
        else
        {
            worker.submit(() -> next.firePipelineComplete(pipeline));
        }
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireExceptionCatch(e);
        }
        else
        {
            worker.submit(() -> next.fireExceptionCatch(e));
        }
    }

    @Override
    public void fireReadClose()
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireReadClose();
        }
        else
        {
            worker.submit(() -> next.fireReadClose());
        }
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireChannelClose(e);
        }
        else
        {
            worker.submit(() -> next.fireChannelClose(e));
        }
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
