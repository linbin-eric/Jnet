package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessor implements WriteProcessor
{
    private final DefaultWriteCompleteHandler writeCompleteHandler;

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
}