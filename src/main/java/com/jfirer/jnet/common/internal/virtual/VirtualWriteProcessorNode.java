package com.jfirer.jnet.common.internal.virtual;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;

public class VirtualWriteProcessorNode implements WriteProcessorNode
{
    private WriteProcessor            processor;
    private VirtualWriteProcessorNode next;

    public VirtualWriteProcessorNode(WriteProcessor processor)
    {
        this.processor = processor;
    }

    @Override
    public void fireWrite(Object data)
    {
        processor.write(data, next);
    }

    @Override
    public void firePipelineComplete(ChannelContext channelContext)
    {
        processor.pipelineComplete(next, channelContext);
    }

    @Override
    public void fireWriteClose()
    {
        processor.writeClose(next);
    }

    @Override
    public void setNext(WriteProcessorNode next)
    {
        this.next = (VirtualWriteProcessorNode) next;
    }

    @Override
    public WriteProcessorNode next()
    {
        return next;
    }

    @Override
    public JnetWorker worker()
    {
        throw new UnsupportedOperationException();
    }
}
