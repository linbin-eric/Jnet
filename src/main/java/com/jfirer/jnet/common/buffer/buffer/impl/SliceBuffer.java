package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.recycler.Recycler;

public abstract class SliceBuffer extends AbstractBuffer
{
    static final Recycler<SliceHeapBuffer>   SLICE_HEAP_BUFFER_RECYCLER   = new Recycler<>(SliceHeapBuffer::new, AbstractBuffer::setRecycleHandler);
    static final Recycler<SliceUnsafeBuffer> SLICE_UNSAFE_BUFFER_RECYCLER = new Recycler<>(SliceUnsafeBuffer::new, AbstractBuffer::setRecycleHandler);
    AbstractBuffer parent;

    protected SliceBuffer(BufferType bufferType)
    {
        super(bufferType);
    }

    public static IoBuffer slice(AbstractBuffer buffer, int length)
    {
        if (buffer.remainRead() < length)
        {
            throw new IllegalArgumentException();
        }
        buffer.incrRef();
        SliceBuffer slice;
        switch (buffer.bufferType)
        {
            case HEAP -> slice = SLICE_HEAP_BUFFER_RECYCLER.get();
            case UNSAFE -> slice = SLICE_UNSAFE_BUFFER_RECYCLER.get();
            case MEMORY, DIRECT -> throw new IllegalArgumentException();
            default -> throw new IllegalStateException("Unexpected value: " + buffer.bufferType);
        }
        slice.parent = buffer;
        slice.init(buffer.memory, length, buffer.readPosi + buffer.offset, buffer.nativeAddress);
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

    public static class SliceHeapBuffer extends SliceBuffer
    {
        protected SliceHeapBuffer()
        {
            super(BufferType.HEAP);
        }
    }

    public static class SliceUnsafeBuffer extends SliceBuffer
    {
        protected SliceUnsafeBuffer()
        {
            super(BufferType.UNSAFE);
        }
    }
}
