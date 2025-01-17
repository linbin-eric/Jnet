package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;

public class TailReadProcessor implements ReadProcessor<Object>
{
    public static final TailReadProcessor INSTANCE = new TailReadProcessor();

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void channelClose(ReadProcessorNode next, Throwable e)
    {
    }

    @Override
    public void exceptionCatch(Throwable e, ReadProcessorNode next)
    {
    }

    @Override
    public void readClose(ReadProcessorNode next)
    {
    }
}
