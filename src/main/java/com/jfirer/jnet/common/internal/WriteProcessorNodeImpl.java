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
    public void fireWriteClose()
    {
        processor.writeClose(next);
    }

    @Override
    public void setNext(WriteProcessorNode next)
    {
        this.next = next;
    }

    @Override
    public WriteProcessorNode getNext()
    {
        return next;
    }
}
