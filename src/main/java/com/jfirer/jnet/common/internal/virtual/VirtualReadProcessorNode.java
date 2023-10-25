package com.jfirer.jnet.common.internal.virtual;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

public class VirtualReadProcessorNode implements ReadProcessorNode
{
    private ReadProcessor            readProcessor;
    private VirtualThreadPipeline    pipeline;
    private VirtualReadProcessorNode next;

    public VirtualReadProcessorNode(ReadProcessor readProcessor, VirtualThreadPipeline pipeline)
    {
        this.readProcessor = readProcessor;
        this.pipeline      = pipeline;
    }

    @Override
    public void fireRead(Object data)
    {
        readProcessor.read(data, next);
    }

    @Override
    public void firePipelineComplete()
    {
        readProcessor.pipelineComplete(next);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readProcessor.exceptionCatch(e, next);
    }

    @Override
    public void fireReadClose()
    {
        readProcessor.readClose(next);
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        readProcessor.channelClose(next, e);
    }

    @Override
    public void setNext(ReadProcessorNode next)
    {
        this.next = (VirtualReadProcessorNode) next;
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
        throw new UnsupportedOperationException();
    }
}
