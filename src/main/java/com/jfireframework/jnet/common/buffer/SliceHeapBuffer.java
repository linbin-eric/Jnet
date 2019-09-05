package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.RecycleHandler;
import com.jfireframework.jnet.common.recycler.Recycler;

public class SliceHeapBuffer extends AbstractHeapBuffer implements SliceBuffer
{
    AbstractBuffer parent;
    static final Recycler<SliceHeapBuffer> RECYCLER = new Recycler<SliceHeapBuffer>()
    {
        @Override
        protected SliceHeapBuffer newObject(RecycleHandler handler)
        {
            SliceHeapBuffer buffer = new SliceHeapBuffer();
            buffer.recycleHandler = handler;
            return buffer;
        }
    };

    @Override
    protected void reAllocate(int posi)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void free0()
    {
        parent.free();
    }

    @Override
    public IoBuffer slice(int length)
    {
        incrRef();
        IoBuffer slice = slice(this, length);
        addReadPosi(length);
        return slice;
    }

    public static IoBuffer slice(AbstractBuffer<byte[]> buffer, int length)
    {
        SliceHeapBuffer slice = RECYCLER.get();
        slice.parent = buffer;
        slice.init(buffer.memory, length, 0, length, buffer.readPosi + buffer.offset);
        return slice;
    }

    @Override
    public IoBuffer getParent()
    {
        return parent;
    }
}
