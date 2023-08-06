package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.storage.CachedStorageSegment;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ThreadCache
{
    static final int                      smallestMask = ~15;
    final        int                      numOfCached;
    private      Arena                    arena;
    private      CachedStorageSegment[][] regionCaches;
    private      long[][]                 bitMaps;
    private      int[]                    numOfAvails;
    private      int[]                    nextAvails;
    private      BufferType               bufferType;

    public ThreadCache(int numOfCached, int maxCachedCapacity, Arena arena, BufferType bufferType)
    {
        this.arena       = arena;
        this.bufferType  = bufferType;
        this.numOfCached = numOfCached;
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

    public CachedStorageSegment allocate(int capacity)
    {
        int size        = normalizeCapacity(capacity);
        int regionIndex = MathUtil.log2(size) - 4;
        if (regionIndex < regionCaches.length)
        {
            CachedStorageSegment[] regionCach = regionCaches[regionIndex];
            long[]                 bitMap     = bitMaps[regionIndex];
            synchronized (regionCach)
            {
                int bitMapIndex = findAvail(regionIndex);
                if (bitMapIndex == -1)
                {
                    CachedStorageSegment cachedStorageSegment = CachedStorageSegment.POOL.get();
                    arena.allocate(size, cachedStorageSegment);
                    return cachedStorageSegment;
                }
                int row = bitMapIndex >>> 6;
                int col = bitMapIndex & 63;
                bitMap[row] |= 1L << col;
                numOfAvails[regionIndex] -= 1;
                CachedStorageSegment cached = regionCach[bitMapIndex];
                if (cached == null)
                {
                    cached = new CachedStorageSegment();
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
                }
                return cached;
            }
        }
        else
        {
            CachedStorageSegment cachedStorageSegment = CachedStorageSegment.POOL.get();
            arena.allocate(size, cachedStorageSegment);
            return cachedStorageSegment;
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
        synchronized (regionCach)
        {
            nextAvails[regionIndex] = bitMapIndex;
            numOfAvails[regionIndex] += 1;
            long[] bitMap = bitMaps[regionIndex];
            int    row    = bitMapIndex >>> 6;
            int    col    = bitMapIndex & 63;
            bitMap[row] ^= 1L << col;
        }
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
