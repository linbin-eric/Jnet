package com.jfireframework.jnet.common.buffer;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.util.MathUtil;

import java.nio.ByteBuffer;

public class ThreadCache
{
    final HeapArena                       heapArena;
    final MemoryRegionCache<byte[]>[]     tinySubPagesHeapCaches;
    final MemoryRegionCache<byte[]>[]     smallSubPageHeapCaches;
    final MemoryRegionCache<byte[]>[]     normalHeapCaches;
    final DirectArena                     directArena;
    final MemoryRegionCache<ByteBuffer>[] tinySubPagesDirectCaches;
    final MemoryRegionCache<ByteBuffer>[] smallSubPagesDirectCaches;
    final MemoryRegionCache<ByteBuffer>[] normalDirectCaches;
    final int                             pagesizeShift;

    public ThreadCache(HeapArena heapArena, DirectArena directArena, int tinyCacheNum, int smallCacheNum, int normalCacheNum, int maxCachedBufferCapacity, int pagesizeShift)
    {
        this.pagesizeShift = pagesizeShift;
        this.heapArena = heapArena;
        tinySubPagesHeapCaches = createSubPageMemoryRegionCache(tinyCacheNum, heapArena.tinySubPages.length);
        smallSubPageHeapCaches = createSubPageMemoryRegionCache(smallCacheNum, heapArena.smallSubPages.length);
        normalHeapCaches = createNormalSizeMemoryRegionCache(normalCacheNum, pagesizeShift, maxCachedBufferCapacity);
        this.directArena = directArena;
        tinySubPagesDirectCaches = createSubPageMemoryRegionCache(tinyCacheNum, directArena.tinySubPages.length);
        smallSubPagesDirectCaches = createSubPageMemoryRegionCache(smallCacheNum, directArena.tinySubPages.length);
        normalDirectCaches = createNormalSizeMemoryRegionCache(normalCacheNum, pagesizeShift, maxCachedBufferCapacity);
        if (heapArena != null)
        {
            heapArena.numThreadCaches.incrementAndGet();
        }
        if (directArena != null)
        {
            directArena.numThreadCaches.incrementAndGet();
        }
        GlobalMetric.watchThreadCache(this);
    }

    @SuppressWarnings("unchecked")
    private <T> MemoryRegionCache<T>[] createSubPageMemoryRegionCache(int cacheSize, int len)
    {
        if (cacheSize > 0)
        {
            MemoryRegionCache<T>[] array = new MemoryRegionCache[len];
            for (int i = 0; i < array.length; i++)
            {
                array[i] = new MemoryRegionCache<>(cacheSize);
            }
            return array;
        }
        else
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> MemoryRegionCache<T>[] createNormalSizeMemoryRegionCache(int cacheNum, int pagesizeShift, int maxCachedBufferCapacity)
    {
        int normalizeSize = MathUtil.normalizeSize(maxCachedBufferCapacity);
        int shift         = MathUtil.log2(normalizeSize);
        int length        = shift - pagesizeShift + 1;
        if (length > 0 && cacheNum > 0)
        {
            MemoryRegionCache<T>[] array = new MemoryRegionCache[length];
            for (int i = 0; i < array.length; i++)
            {
                array[i] = new MemoryRegionCache<>(cacheNum);
            }
            return array;
        }
        else
        {
            return null;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean allocate(PooledBuffer<?> buffer, int normalizeCapacity, SizeType sizeType, Arena<?> arena)
    {
        MemoryRegionCache<?> cache = findCache(normalizeCapacity, sizeType, arena);
        if (cache == null)
        {
            return false;
        }
        return ((MemoryRegionCache) cache).tryFindAndInitBuffer(buffer, this);
    }

    MemoryRegionCache<?> findCache(int normalizeCapacity, SizeType sizeType, Arena<?> arena)
    {
        MemoryRegionCache<?> cache = null;
        switch (sizeType)
        {
            case TINY:
                cache = cacheForTiny(normalizeCapacity, arena);
                break;
            case SMALL:
                cache = cacheForSmall(normalizeCapacity, arena);
                break;
            case NORMAL:
                cache = cacheForNormal(normalizeCapacity, arena);
                break;
            default:
                ReflectUtil.throwException(new NullPointerException());
        }
        return cache;
    }

    MemoryRegionCache<?> cacheForTiny(int normalizeCapacity, Arena<?> arena)
    {
        int tinyIdx = Arena.tinyIdx(normalizeCapacity);
        if (arena.isDirect())
        {
            if (tinySubPagesDirectCaches == null || tinySubPagesDirectCaches.length <= tinyIdx)
            {
                return null;
            }
            return tinySubPagesDirectCaches[tinyIdx];
        }
        else
        {
            if (tinySubPagesHeapCaches == null || tinySubPagesHeapCaches.length <= tinyIdx)
            {
                return null;
            }
            return tinySubPagesHeapCaches[tinyIdx];
        }
    }

    Arena<?> arena(boolean direct)
    {
        if (direct)
        {
            return directArena;
        }
        else
        {
            return heapArena;
        }
    }

    MemoryRegionCache<?> cacheForSmall(int normalizeCapacity, Arena<?> arena)
    {
        int smallIdx = Arena.smallIdx(normalizeCapacity);
        if (arena.isDirect())
        {
            if (smallSubPagesDirectCaches == null || smallSubPagesDirectCaches.length <= smallIdx)
            {
                return null;
            }
            return smallSubPagesDirectCaches[smallIdx];
        }
        else
        {
            if (smallSubPageHeapCaches == null || smallSubPageHeapCaches.length <= smallIdx)
            {
                return null;
            }
            return smallSubPageHeapCaches[smallIdx];
        }
    }

    MemoryRegionCache<?> cacheForNormal(int normalizeCapacity, Arena<?> arena)
    {
        int shift = MathUtil.log2(normalizeCapacity);
        int idx   = shift - pagesizeShift;
        if (arena.isDirect())
        {
            if (normalDirectCaches == null || normalDirectCaches.length <= idx)
            {
                return null;
            }
            return normalDirectCaches[idx];
        }
        else
        {
            if (normalHeapCaches == null || normalHeapCaches.length <= idx)
            {
                return null;
            }
            return normalHeapCaches[idx];
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean add(int normalizeCapacity, SizeType sizeType, Arena<?> arena, Chunk<?> chunk, long handle)
    {
        MemoryRegionCache<?> cache = findCache(normalizeCapacity, sizeType, arena);
        if (cache == null)
        {
            return false;
        }
        return ((MemoryRegionCache) cache).offer(chunk, handle);
    }
}
