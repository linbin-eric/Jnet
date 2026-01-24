package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;

public class WriteProcessorNodeImpl implements WriteProcessorNode
{
    protected WriteProcessor     processor;
    protected WriteProcessorNode next;
    protected Pipeline           pipeline;

    public WriteProcessorNodeImpl(WriteProcessor processor, Pipeline pipeline)
    {
        this.processor = processor;
        this.pipeline  = pipeline;
    }

    @Override
    public void fireWrite(Object data)
    {
        processor.write(data, next);
    }

    @Override
    public void fireChannelClosed(Throwable e)
    {
        processor.channelClosed(next, e);
    }

    @Override
    public WriteProcessorNode getNext()
    {
        return next;
    }

    @Override
    public void setNext(WriteProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
