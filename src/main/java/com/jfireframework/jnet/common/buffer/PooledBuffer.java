package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.recycler.RecycleHandler;

public abstract class PooledBuffer<T> extends AbstractBuffer<T>
{
    // 可用位置相对于memory的偏移量
    long           handle;
    ThreadCache    cache;
    Chunk<T>       chunk;


    public void init(Chunk<T> chunk, int capacity, int offset, long handle, ThreadCache cache)
    {
        this.handle = handle;
        this.cache = cache;
        this.chunk = chunk;
        init(chunk.memory, capacity, 0, 0, offset);
//        memory = chunk.memory;
//        this.offset = offset;
//        this.capacity = capacity;
//        readPosi = writePosi = 0;
//        refCount = 1;

    }

    public void initUnPooled(Chunk<T> chunk, ThreadCache cache)
    {
        this.chunk = chunk;
        this.cache = cache;
        handle = -1;
        init(chunk.memory, chunk.chunkSize,0,0,0);
//        memory = chunk.memory;
//        offset = 0;
//        readPosi = writePosi = 0;
//        capacity = chunk.chunkSize;
//        refCount = 1;
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        chunk.arena.reAllocate(this, reqCapacity);
    }

    @Override
    public IoBuffer put(IoBuffer buffer, int len)
    {
        if (buffer.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        if (buffer instanceof PooledBuffer)
        {
            put0((PooledBuffer) buffer, len);
        }
        else
        {
            int originReadPosi = buffer.getReadPosi();
            for (int i = 0; i < len; i++)
            {
                put(buffer.get());
            }
            buffer.setReadPosi(originReadPosi);
        }
        return this;
    }

    protected abstract void put0(PooledBuffer buffer, int len);

    @Override
    public void free()
    {
        if (descRef() > 0)
        {
            return;
        }
        chunk.arena.free(chunk, handle, capacity, cache);
        handle = offset = capacity = readPosi = writePosi = 0;
        memory = null;
        cache = null;
        chunk = null;
        if (recycleHandler != null)
        {
            recycleHandler.recycle(this);
        }

    }

    @Override
    public IoBuffer slice(int length)
    {
        return null;
    }
}
