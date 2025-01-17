package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.WriteCompletionHandler;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessorImpl implements WriteProcessor
{
    private WriteCompletionHandler writeCompleteHandler;

    public TailWriteProcessorImpl(Pipeline pipeline)
    {
        writeCompleteHandler = new DefaultWriteCompleteHandler(pipeline);
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
}
