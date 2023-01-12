package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.SliceBuffer;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class SliceDirectBuffer extends AbstractDirectBuffer implements SliceBuffer
{
    AbstractBuffer parent;
    private      RecycleHandler<SliceDirectBuffer> sliceRecycleHandler;
    static final Recycler<SliceDirectBuffer>       RECYCLER = new Recycler<>(function -> {
        SliceDirectBuffer buffer = new SliceDirectBuffer();
        buffer.sliceRecycleHandler = function.apply(buffer);
        return buffer;
    });

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
        sliceRecycleHandler.recycle(this);
    }

    @Override
    public IoBuffer slice(int length)
    {
        return slice(this, length);
    }

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
    public IoBuffer getParent()
    {
        return parent;
    }
}
