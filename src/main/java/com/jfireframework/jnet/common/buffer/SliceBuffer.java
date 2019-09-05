package com.jfireframework.jnet.common.buffer;

public abstract class SliceBuffer<T> extends AbstractBuffer<T>
{
    AbstractBuffer root;
    AbstractBuffer parent;

    @Override
    protected void reAllocate(int posi)
    {
        throw new UnsupportedOperationException();
    }
}
