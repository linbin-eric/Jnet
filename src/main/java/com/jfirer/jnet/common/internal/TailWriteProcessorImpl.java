package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ChannelContext;
import com.jfirer.jnet.common.api.WriteCompletionHandler;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessorImpl implements WriteProcessor
{
    private WriteCompletionHandler writeCompleteHandler;

    public TailWriteProcessorImpl(ChannelContext channelContext)
    {
        writeCompleteHandler = new DefaultWriteCompleteHandler(channelContext);
    }

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        writeCompleteHandler.write((IoBuffer) data);
    }

    @Override
    public void writeClose(WriteProcessorNode next)
    {
        ;
    }

    @Override
    public void pipelineComplete(WriteProcessorNode next, ChannelContext channelContext)
    {
        ;
    }
}
