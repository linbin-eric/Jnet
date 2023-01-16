package com.jfirer.jnet.common.buffer.buffer;

import com.jfirer.jnet.common.buffer.ThreadCache;

public interface CachedPooledBuffer<T> extends PooledBuffer<T>
{
    void init(ThreadCache.MemoryCached<T> memoryCached, ThreadCache<T> cache);

    void setCache(ThreadCache<T> cache);
}
