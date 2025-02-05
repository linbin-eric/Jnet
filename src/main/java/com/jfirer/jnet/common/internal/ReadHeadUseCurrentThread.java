package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessorNode;

import java.util.function.Consumer;

class ReadHeadUseCurrentThread implements ReadProcessorNode
{
    protected final Pipeline            pipeline;
    protected       ReadProcessorNode   next;
    protected       Consumer<Throwable> jvmExistHandler = e -> {
        System.err.println("Some RunnableImpl run in Jnet not handle Exception well,Check all ReadProcessor and WriteProcessor");
        e.printStackTrace();
    };

    ReadHeadUseCurrentThread(Pipeline pipeline)
    {
        this.pipeline   = pipeline;
        jvmExistHandler = pipeline.channelConfig().getJvmExistHandler();
    }

    @Override
    public void fireRead(Object data)
    {
        try
        {
            next.fireRead(data);
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(129);
        }
    }

    @Override
    public void fireReadFailed(Throwable e)
    {
        try
        {
            next.fireReadFailed(e);
        }
        catch (Throwable eOfJvmExit)
        {
            jvmExistHandler.accept(eOfJvmExit);
            System.exit(129);
        }
    }

    @Override
    public void firePipelineComplete(Pipeline pipeline)
    {
        try
        {
            next.firePipelineComplete(pipeline);
        }
        catch (Throwable e)
        {
            jvmExistHandler.accept(e);
            System.exit(129);
        }
    }

    @Override
    public void setNext(ReadProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public ReadProcessorNode getNext()
    {
        return next;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
