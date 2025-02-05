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
    public void fireReadFailed(Throwable e)
    {
        processor.readFailed(e, next);
    }

    @Override
    public void firePipelineComplete(Pipeline pipeline)
    {
        processor.pipelineComplete(pipeline, next);
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
