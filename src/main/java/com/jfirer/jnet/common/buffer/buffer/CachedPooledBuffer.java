package com.jfirer.jnet.common.buffer.buffer;

import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.buffer.buffer.impl.CachedPooledDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CachedPooledHeapBuffer;

import java.nio.ByteBuffer;

public interface CachedPooledBuffer<T> extends PooledBuffer<T>
{
    void init(ThreadCache.MemoryCached<T> memoryCached, ThreadCache<T> cache);

    void setCache(ThreadCache<T> cache);

    static CachedPooledBuffer<ByteBuffer> allocateDirect()
    {
        return CachedPooledDirectBuffer.newOne();
    }

    static CachedPooledBuffer<byte[]> allocate()
    {
        return CachedPooledHeapBuffer.newOne();
    }
}
