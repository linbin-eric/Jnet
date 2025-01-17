package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.WriteCompletionHandler;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessor implements WriteProcessor
{
    private WriteCompletionHandler writeCompleteHandler;

    public TailWriteProcessor(Pipeline pipeline)
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
