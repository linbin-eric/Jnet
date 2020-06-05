package com.jfirer.jnet.common.decoder;

import com.jfirer.jnet.common.api.ProcessorContext;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.buffer.BufferAllocator;
import com.jfirer.jnet.common.buffer.IoBuffer;

public abstract class AbstractDecoder implements ReadProcessor
{
    protected BufferAllocator allocator;
    protected IoBuffer        accumulation;

    public AbstractDecoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    public void read(Object data, ProcessorContext ctx)
    {
        if (accumulation == null)
        {
            accumulation = (IoBuffer) data;
        }
        else
        {
            accumulation.put((IoBuffer) data);
            ((IoBuffer) data).free();
        }
        process0(ctx);
    }

    protected abstract void process0(ProcessorContext ctx);

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

    @Override
    public void endOfReadLife(ProcessorContext next)
    {
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
    }
}
