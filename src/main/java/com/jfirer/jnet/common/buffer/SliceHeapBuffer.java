package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;

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
    protected long getAddress(byte[] memory)
    {
        return 0;
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
    }

    @Override
    public IoBuffer slice(int length)
    {
        return slice(this, length);
    }

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
//        slice.init(buffer.memory, length, 0, length, buffer.readPosi + buffer.offset);
        buffer.addReadPosi(length);
        return slice;
    }

    @Override
    public IoBuffer getParent()
    {
        return parent;
    }
}
