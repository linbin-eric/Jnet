package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;

public class WriteProcessorNodeImpl implements WriteProcessorNode
{
    protected WriteProcessor     processor;
    protected WriteProcessorNode next;

    public WriteProcessorNodeImpl(WriteProcessor processor)
    {
        this.processor = processor;
    }

    @Override
    public void fireWrite(Object data)
    {
        processor.write(data, next);
    }

    @Override
    public void fireChannelClosed()
    {
        processor.channelClosed(next);
    }

    @Override
    public void fireWriteFailed(Throwable e)
    {
        processor.writeFailed(next, e);
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
}
