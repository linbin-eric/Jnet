package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

public class ReadProcessorNodeImpl implements ReadProcessorNode
{
    private final ReadProcessor     processor;
    private final Pipeline          pipeline;
    private       ReadProcessorNode next;

    public ReadProcessorNodeImpl(ReadProcessor processor, Pipeline pipeline)
    {
        this.processor = processor;
        this.pipeline  = pipeline;
    }

    @Override
    public void fireRead(Object data)
    {
        processor.read(data, next);
    }

    @Override
    public void firePipelineComplete(Pipeline pipeline)
    {
        processor.pipelineComplete(pipeline);
        if (next != null)
        {
            next.firePipelineComplete(pipeline);
        }
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        processor.exceptionCatch(e, next);
    }

    @Override
    public void fireReadClose()
    {
        processor.readClose(next);
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        processor.channelClose(next, e);
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
}
