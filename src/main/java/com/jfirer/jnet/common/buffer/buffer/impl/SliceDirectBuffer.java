package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class SliceDirectBuffer extends AbstractDirectBuffer
{
    static final Recycler<SliceDirectBuffer> RECYCLER = new Recycler<>(function -> {
        SliceDirectBuffer buffer = new SliceDirectBuffer();
        buffer.setRecycleHandler(function.apply(buffer));
        return buffer;
    });
    AbstractBuffer parent;

    public static IoBuffer slice(AbstractBuffer<ByteBuffer> buffer, int length)
    {
        if (buffer.remainRead() < length)
        {
            throw new IllegalArgumentException();
        }
        buffer.incrRef();
        SliceDirectBuffer slice = RECYCLER.get();
        slice.parent = buffer;
        slice.init(buffer.memory, length, buffer.offset + buffer.readPosi);
        slice.addWritePosi(length);
        buffer.addReadPosi(length);
        return slice;
    }

    @Override
    protected long getAddress(ByteBuffer memory)
    {
        return parent.getAddress(memory);
    }

    @Override
    protected void reAllocate(int posi)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void free0(int capacity)
    {
        parent.free();
        parent = null;
    }
}
