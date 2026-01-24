package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.WriteCompletionHandler;
import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

public class TailWriteProcessor implements WriteProcessor
{
    private final WriteCompletionHandler writeCompleteHandler;

    public TailWriteProcessor(WriteCompletionHandler writeCompleteHandler)
    {
        this.writeCompleteHandler = writeCompleteHandler;
    }

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        writeCompleteHandler.write((IoBuffer) data);
    }

    @Override
    public void channelClosed(WriteProcessorNode next, Throwable e)
    {
        ;
    }
}