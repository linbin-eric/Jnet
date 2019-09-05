package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.CapacityStat;
import com.jfireframework.jnet.common.util.MathUtil;
import com.jfireframework.jnet.common.util.ReflectUtil;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class Arena<T>
{
    final int pageSize;
    final int pageSizeShift;
    final int maxLevel;
    final int subpageOverflowMask;
    final int chunkSize;
    ChunkList<T>          c000;
    ChunkList<T>          c025;
    ChunkList<T>          c050;
    ChunkList<T>          c075;
    ChunkList<T>          c100;
    ChunkList<T>          cInt;
    SubPage<T>[]          tinySubPages;
    SubPage<T>[]          smallSubPages;
    // 有多少ThreadCache持有了该Arena
    AtomicInteger         numThreadCaches = new AtomicInteger(0);
    PooledBufferAllocator parent;
    /**
     * 统计相关
     **/
    int                   newChunkCount   = 0;
    int                   hugeChunkCount  = 0;
    String                name;

    @SuppressWarnings("unchecked")
    public Arena(final PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask, final String name)
    {
        if (pageSize < 4096)
        {
            ReflectUtil.throwException(new IllegalArgumentException("pagesize不能小于4096"));
        }
        this.parent = parent;
        this.maxLevel = maxLevel;
        this.pageSize = pageSize;
        this.pageSizeShift = pageSizeShift;
        this.subpageOverflowMask = subpageOverflowMask;
        this.name = name;
        chunkSize = (1 << maxLevel) * pageSize;
        c100 = new ChunkList<>(100, 100, null, chunkSize);
        c075 = new ChunkList<>(75, 99, c100, chunkSize);
        c050 = new ChunkList<>(50, 99, c075, chunkSize);
        c025 = new ChunkList<>(25, 75, c050, chunkSize);
        c000 = new ChunkList<>(1, 50, c025, chunkSize);
        cInt = new ChunkList<>(0, 25, c000, chunkSize);
        c100.setPrevList(c075);
        c075.setPrevList(c050);
        c050.setPrevList(c025);
        c025.setPrevList(c000);
        // 在tiny区间，以16为大小，每一个16的倍数都占据一个槽位。为了方便定位，实际上数组的0下标是不使用的
        tinySubPages = new SubPage[512 >>> 4];
        for (int i = 0; i < tinySubPages.length; i++)
        {
            tinySubPages[i] = new SubPage<T>(pageSize);
        }
        // 在small，从1<<9开始，每一次右移都占据一个槽位，直到pagesize大小.
        smallSubPages = new SubPage[pageSizeShift - 9];
        for (int i = 0; i < smallSubPages.length; i++)
        {
            smallSubPages[i] = new SubPage<T>(pageSize);
        }
    }

    public int tinySubPageNum()
    {
        return tinySubPages.length;
    }

    public int smallSubPageNum()
    {
        return smallSubPages.length;
    }

    static int tinyIdx(int normalizeCapacity)
    {
        return normalizeCapacity >>> 4;
    }

    static int smallIdx(int normalizeCapacity)
    {
        return MathUtil.log2(normalizeCapacity) - 9;
    }

    static boolean isTiny(int normCapacity)
    {
        return (normCapacity & 0xFFFFFE00) == 0;
    }

    abstract Chunk<T> newChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask);

    abstract Chunk<T> newChunk(int reqCapacity);

    void allocateHuge(int reqCapacity, PooledBuffer<T> buffer, ThreadCache cache)
    {
        Chunk<T> hugeChunk = newChunk(reqCapacity);
        hugeChunk.arena = this;
        hugeChunk.unPoooledChunkInitBuf(buffer, cache);
        hugeChunkCount++;
    }

    abstract void destoryChunk(Chunk<T> chunk);

    public void allocate(int reqCapacity, int maxCapacity, PooledBuffer<T> buffer, ThreadCache cache)
    {
        int normalizeCapacity = normalizeCapacity(reqCapacity);
        if (isTinyOrSmall(normalizeCapacity))
        {
            SubPage<T> head;
            if (isTiny(normalizeCapacity))
            {
                if (cache.allocate(buffer, normalizeCapacity, SizeType.TINY, isDirect()))
                {
                    return;
                }
                head = tinySubPages[tinyIdx(normalizeCapacity)];
            }
            else
            {
                if (cache.allocate(buffer, normalizeCapacity, SizeType.SMALL, isDirect()))
                {
                    return;
                }
                head = smallSubPages[smallIdx(normalizeCapacity)];
            }
            synchronized (head)
            {
                SubPage<T> succeed = head.next;
                if (succeed != head)
                {
                    long handle = succeed.allocate();
                    succeed.chunk.initBuf(handle, buffer, cache);
                    return;
                }
            }
            allocateNormal(buffer, normalizeCapacity, cache);
        }
        else if (normalizeCapacity <= chunkSize)
        {
            if (cache.allocate(buffer, normalizeCapacity, SizeType.NORMAL, isDirect()))
            {
                return;
            }
            allocateNormal(buffer, normalizeCapacity, cache);
        }
        else
        {
            allocateHuge(reqCapacity, buffer, cache);
        }
    }

    private synchronized void allocateNormal(PooledBuffer<T> buffer, int normalizeCapacity, ThreadCache cache)
    {
        if (//
                c050.allocate(normalizeCapacity, buffer, cache)//
                        || c025.allocate(normalizeCapacity, buffer, cache)//
                        || c000.allocate(normalizeCapacity, buffer, cache)//
                        || cInt.allocate(normalizeCapacity, buffer, cache)//
                        || c075.allocate(normalizeCapacity, buffer, cache))
        {
            return;
        }
        Chunk<T> chunk = newChunk(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
        chunk.arena = this;
        long handle = chunk.allocate(normalizeCapacity);
        assert handle > 0;
        chunk.initBuf(handle, buffer, cache);
        cInt.addFromPrev(chunk, chunk.usage());
        newChunkCount++;
    }

    public void reAllocate(PooledBuffer<T> buffer, int newReqCapacity)
    {
        Chunk<T>    oldChunk     = buffer.chunk;
        long        oldHandle    = buffer.handle;
        int         oldReadPosi  = buffer.readPosi;
        int         oldWritePosi = buffer.writePosi;
        int         oldCapacity  = buffer.capacity;
        int         oldOffset    = buffer.offset;
        T           oldMemory    = buffer.memory;
        ThreadCache oldCache     = buffer.cache;
        allocate(newReqCapacity, Integer.MAX_VALUE, buffer, parent.threadCache());
        if (newReqCapacity > oldCapacity)
        {
            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
            memoryCopy(oldMemory, oldOffset, buffer.memory, buffer.offset, oldWritePosi);
        }
        // 这种情况是缩小，目前还不支持
        else
        {
            ReflectUtil.throwException(new UnsupportedOperationException());
        }
        free(oldChunk, oldHandle, oldCapacity, oldCache);
    }

    abstract void memoryCopy(T src, int srcOffset, T desc, int destOffset, int oldWritePosi);

    int normalizeCapacity(int reqCapacity)
    {
        if (reqCapacity >= chunkSize)
        {
            return reqCapacity;
        }
        if (isTiny(reqCapacity))
        {
            return (reqCapacity & 15) == 0 ? reqCapacity : (reqCapacity & ~15) + 16;
        }
        return MathUtil.normalizeSize(reqCapacity);
    }

    boolean isTinyOrSmall(int normCapacity)
    {
        return (normCapacity & subpageOverflowMask) == 0;
    }

    public void free(Chunk<T> chunk, long handle, int normalizeCapacity, ThreadCache cache)
    {
        if (chunk.unpooled == true)
        {
            destoryChunk(chunk);
        }
        else
        {
            if (cache.add(normalizeCapacity, sizeType(normalizeCapacity), isDirect(), chunk, handle))
            {
                return;
            }
            final boolean destoryChunk;
            synchronized (this)
            {
                destoryChunk = chunk.parent.free(chunk, handle);
            }
            if (destoryChunk)
            {
                destoryChunk(chunk);
            }
        }
    }

    SizeType sizeType(int normalizeCapacity)
    {
        if (isTinyOrSmall(normalizeCapacity))
        {
            if (isTiny(normalizeCapacity))
            {
                return SizeType.TINY;
            }
            else
            {
                return SizeType.SMALL;
            }
        }
        return SizeType.NORMAL;
    }

    SubPage<T> findSubPageHead(int elementSize)
    {
        if (isTiny(elementSize))
        {
            int tinyIdx = tinyIdx(elementSize);
            return tinySubPages[tinyIdx];
        }
        else
        {
            int smallIdx = smallIdx(elementSize);
            return smallSubPages[smallIdx];
        }
    }

    public abstract boolean isDirect();

    public void capacityStat(CapacityStat capacityStat)
    {
        cInt.stat(capacityStat);
        c025.stat(capacityStat);
        c050.stat(capacityStat);
        c075.stat(capacityStat);
        c100.stat(capacityStat);
    }
}
