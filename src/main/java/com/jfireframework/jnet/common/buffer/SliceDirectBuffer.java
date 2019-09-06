package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.RecycleHandler;
import com.jfireframework.jnet.common.recycler.Recycler;

import java.nio.ByteBuffer;

public class SliceDirectBuffer extends AbstractDirectBuffer implements SliceBuffer
{
    AbstractBuffer parent;
    static final Recycler<SliceDirectBuffer> RECYCLER = new Recycler<SliceDirectBuffer>()
    {
        @Override
        protected SliceDirectBuffer newObject(RecycleHandler handler)
        {
            SliceDirectBuffer buffer = new SliceDirectBuffer();
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
        slice.init(buffer.memory, length, 0, length, buffer.readPosi + buffer.offset);
        buffer.addReadPosi(length);
        return slice;
    }

    @Override
    public IoBuffer getParent()
    {
        return parent;
    }
}
