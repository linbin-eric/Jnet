package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessor implements WriteProcessor
{
    private DefaultWriteCompleteHandler writeCompleteHandler;

    public TailWriteProcessor(DefaultWriteCompleteHandler writeCompleteHandler)
    {
        this.writeCompleteHandler = writeCompleteHandler;
    }

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        writeCompleteHandler.write((IoBuffer) data);
    }

    @Override
    public void writeFailed(WriteProcessorNode next, Throwable e)
    {
        ;
    }

    @Override
    public void channelClosed(WriteProcessorNode next)
    {
        ;
    }
}