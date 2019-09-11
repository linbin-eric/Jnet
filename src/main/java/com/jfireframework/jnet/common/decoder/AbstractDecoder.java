package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

public abstract class AbstractDecoder extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    protected BufferAllocator allocator;
    protected IoBuffer        accumulation;

    public AbstractDecoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    public void process(IoBuffer buffer) throws Throwable
    {
        if (accumulation == null)
        {
            accumulation = buffer;
        }
        else
        {
            accumulation.put(buffer);
            buffer.free();
        }
        process0();
    }

    protected abstract void process0() throws Throwable;

    protected void compactIfNeed()
    {
        if (accumulation.remainRead() > (accumulation.capacity() >> 2))
        {
            return;
        }
        if (accumulation.refCount() > 1)
        {
            IoBuffer newAcc = allocator.ioBuffer(accumulation.capacity());
            newAcc.put(accumulation);
            accumulation.free();
            accumulation = newAcc;
        }
        else
        {
            accumulation.compact();
        }
    }
}
