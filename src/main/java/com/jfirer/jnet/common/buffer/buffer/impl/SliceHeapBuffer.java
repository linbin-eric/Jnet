package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.recycler.Recycler;

public class SliceHeapBuffer extends CacheablePoolableHeapBuffer
{
    static final Recycler<SliceHeapBuffer> RECYCLER = new Recycler<>(recycleHandlerFunction -> {
        SliceHeapBuffer buffer = new SliceHeapBuffer();
        buffer.setRecycleHandler(recycleHandlerFunction.apply(buffer));
        return buffer;
    });
    AbstractBuffer parent;

    public static IoBuffer slice(AbstractBuffer<byte[]> buffer, int length)
    {
        if (buffer.remainRead() < length)
        {
            throw new IllegalArgumentException();
        }
        buffer.incrRef();
        SliceHeapBuffer slice = RECYCLER.get();
        slice.parent = buffer;
        slice.init(buffer.memory, length, buffer.readPosi + buffer.offset);
        slice.addWritePosi(length);
        buffer.addReadPosi(length);
        return slice;
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
