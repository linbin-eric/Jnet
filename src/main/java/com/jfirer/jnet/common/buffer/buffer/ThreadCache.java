package com.jfirer.jnet.common.buffer.buffer;

import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.impl.CachedBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.storage.CachedStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ThreadCache
{
    static final  int                      smallestMask = ~15;
    final         int                      numOfCached;
    private final Arena                    arena;
    private final CachedStorageSegment[][] regionCaches;
    private final long[][]                 bitMaps;
    private final int[]                    numOfAvails;
    private final int[]                    nextAvails;
    private final BufferType               bufferType;
    private final Lock                     lock         = new ReentrantLock();
    private       CachedBufferAllocator    allocator;

    public ThreadCache(int numOfCached, int maxCachedCapacity, Arena arena, BufferType bufferType,CachedBufferAllocator allocator)
    {
        this.arena       = arena;
        this.bufferType  = bufferType;
        this.numOfCached = numOfCached;
        this.allocator = allocator;
        regionCaches     = new CachedStorageSegment[MathUtil.log2(maxCachedCapacity) - 4][];
        bitMaps          = new long[regionCaches.length][];
        numOfAvails      = new int[bitMaps.length];
        nextAvails       = new int[bitMaps.length];
        Arrays.fill(numOfAvails, this.numOfCached);
        Arrays.fill(nextAvails, -1);
        int bitMapLength = (numOfCached & 63) == 0 ? numOfCached >>> 6 : ((numOfCached >>> 6) + 1);
        for (int i = 0; i < regionCaches.length; i++)
        {
            regionCaches[i] = new CachedStorageSegment[this.numOfCached];
            bitMaps[i]      = new long[bitMapLength];
        }
    }

    public StorageSegment allocate(int capacity)
    {
        int size        = normalizeCapacity(capacity);
        int regionIndex = MathUtil.log2(size) - 4;
        if (regionIndex < regionCaches.length)
        {
            CachedStorageSegment[] regionCach = regionCaches[regionIndex];
            long[]                 bitMap     = bitMaps[regionIndex];
            lock.lock();
            try
            {
                int bitMapIndex = findAvail(regionIndex);
                if (bitMapIndex == -1)
                {
                    PooledStorageSegment pooledStorageSegment = (PooledStorageSegment) allocator.storageSegmentInstance();
                    arena.allocate(size, pooledStorageSegment);
                    return pooledStorageSegment;
                }
                int row = bitMapIndex >>> 6;
                int col = bitMapIndex & 63;
                bitMap[row] |= 1L << col;
                numOfAvails[regionIndex] -= 1;
                CachedStorageSegment cached = regionCach[bitMapIndex];
                if (cached == null)
                {
                    cached = new CachedStorageSegment(allocator);
                    cached.setThreadCache(this);
                    cached.setBitMapIndex(bitMapIndex);
                    cached.setRegionIndex(regionIndex);
                    switch (bufferType)
                    {
                        case HEAP ->
                        {
                            cached.init(new byte[size], 0, 0, size);
                        }
                        case DIRECT, MEMORY ->
                        {
                            throw new UnsupportedOperationException();
                        }
                        case UNSAFE ->
                        {
                            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(size);
                            cached.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, size);
                        }
                    }
                    regionCach[bitMapIndex] = cached;
                    LeakDetecter.LeakTracker watch = cached.getWatch();
                    //因为当ThreadCache 所在的线程死亡被销毁，对应的 ThreadCache 就会被触发 GC 回收。而 ThreadCache中的申请的空间都不是池化的，因此不需要触发泄露监控。在这里直接关闭对应的泄露监控就能避免误报。
                    watch.close();
                }
                return cached;
            }
            finally
            {
                lock.unlock();
            }
        }
        else
        {
            PooledStorageSegment pooledStorageSegment = (PooledStorageSegment) allocator.storageSegmentInstance();
            arena.allocate(size, pooledStorageSegment);
            return pooledStorageSegment;
        }
    }

    private int findAvail(int index)
    {
        if (numOfAvails[index] == 0)
        {
            return -1;
        }
        int nextAvail = nextAvails[index];
        if (nextAvail != -1)
        {
            nextAvails[index] = -1;
            return nextAvail;
        }
        else
        {
            long[] bitMap = bitMaps[index];
            for (int i = 0; i < bitMap.length; i++)
            {
                long bits = bitMap[i];
                if (~bits != 0)
                {
                    int bitMapIndex = i << 6;
                    for (int j = 0; j < 64 && bitMapIndex < numOfCached; j++)
                    {
                        if ((bits & 1) == 0)
                        {
                            return bitMapIndex;
                        }
                        else
                        {
                            bits >>>= 1;
                            bitMapIndex += 1;
                        }
                    }
                    return -1;
                }
            }
            return -1;
        }
    }

    public void free(int regionIndex, int bitMapIndex)
    {
        CachedStorageSegment[] regionCach = regionCaches[regionIndex];
        lock.lock();
        nextAvails[regionIndex] = bitMapIndex;
        numOfAvails[regionIndex] += 1;
        long[] bitMap = bitMaps[regionIndex];
        int    row    = bitMapIndex >>> 6;
        int    col    = bitMapIndex & 63;
        bitMap[row] ^= 1L << col;
        lock.unlock();
    }

    int normalizeCapacity(int reqCapacity)
    {
        if ((reqCapacity & smallestMask) == 0)
        {
            return 16;
        }
        return MathUtil.normalizeSize(reqCapacity);
    }
}
