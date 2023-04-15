package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ThreadCache
{
    public static class MemoryCached
    {
        public Arena         arena;
        public ChunkListNode chunkListNode;
        public int           capacity;
        public int           offset;
        public long          handle;
        public int           bitMapIndex;
        int regionIndex;
    }

    static final int              smallestMask = ~15;
    final        int              numOfCached;
    private      Arena            arena;
    private      MemoryCached[][] regionCaches;
    private      long[][]         bitMaps;
    private      int[]            numOfAvails;
    private      int[]            nextAvails;

    public ThreadCache(int numOfCached, int maxCachedCapacity, Arena arena)
    {
        this.arena = arena;
        this.numOfCached = numOfCached;
        regionCaches = new MemoryCached[MathUtil.log2(maxCachedCapacity) - 4][];
        bitMaps = new long[regionCaches.length][];
        numOfAvails = new int[bitMaps.length];
        nextAvails = new int[bitMaps.length];
        Arrays.fill(numOfAvails, this.numOfCached);
        Arrays.fill(nextAvails, -1);
        int bitMapLength = (numOfCached & 63) == 0 ? numOfCached >>> 6 : ((numOfCached >>> 6) + 1);
        for (int i = 0; i < regionCaches.length; i++)
        {
            regionCaches[i] = new MemoryCached[this.numOfCached];
            bitMaps[i] = new long[bitMapLength];
        }
    }

    public void reAllocate(MemoryCached oldMemoryCache, int newReqCapacity, CacheablePooledBuffer buffer)
    {
        int    oldReadPosi  = buffer.getReadPosi();
        int    oldWritePosi = buffer.getWritePosi();
        int    oldCapacity  = buffer.capacity();
        int    oldOffset    = buffer.offset();
        Object oldMemory    = buffer.memory();
        if (newReqCapacity > oldCapacity)
        {
            allocate(newReqCapacity, buffer);
            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
            switch (buffer.bufferType())
            {
                case HEAP -> System.arraycopy(oldMemory, oldOffset, buffer.memory(), buffer.offset(), oldWritePosi);
                case DIRECT, UNSAFE ->
                        Bits.copyDirectMemory(PlatFormFunction.bytebufferOffsetAddress((ByteBuffer) oldMemory) + oldOffset, PlatFormFunction.bytebufferOffsetAddress((ByteBuffer) buffer.memory()) + buffer.offset(), oldWritePosi);
                default -> throw new IllegalArgumentException();
            }
        }
        // 这种情况是缩小，目前还不支持
        else
        {
            ReflectUtil.throwException(new UnsupportedOperationException());
        }
        free(oldMemoryCache);
    }

    public void allocate(int capacity, CacheablePooledBuffer buffer)
    {
        int size  = normalizeCapacity(capacity);
        int index = MathUtil.log2(size) - 4;
        if (index < regionCaches.length)
        {
            MemoryCached[] regionCach = regionCaches[index];
            long[]         bitMap     = bitMaps[index];
            synchronized (regionCach)
            {
                int bitMapIndex = findAvail(index);
                if (bitMapIndex == -1)
                {
                    arena.allocate(capacity, buffer);
                    return;
                }
                int row = bitMapIndex >>> 6;
                int col = bitMapIndex & 63;
                bitMap[row] |= 1L << col;
                numOfAvails[index] -= 1;
                MemoryCached memoryCached = regionCach[bitMapIndex];
                if (memoryCached == null)
                {
                    MemoryFetch memoryFetch = new MemoryFetch(buffer.bufferType());
                    arena.allocate(size, memoryFetch);
                    memoryCached = new MemoryCached();
                    memoryCached.arena = arena;
                    memoryCached.chunkListNode = memoryFetch.fetchNode();
                    memoryCached.capacity = size;
                    memoryCached.offset = memoryFetch.fetchOffset();
                    memoryCached.handle = memoryFetch.fetchHandle();
                    memoryCached.bitMapIndex = bitMapIndex;
                    memoryCached.regionIndex = index;
                    regionCach[bitMapIndex] = memoryCached;
                }
                buffer.init(memoryCached, this);
            }
        }
        else
        {
            arena.allocate(capacity, buffer);
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

    public void free(MemoryCached memoryCached)
    {
        int            regionIndex = memoryCached.regionIndex;
        MemoryCached[] regionCach  = regionCaches[regionIndex];
        synchronized (regionCach)
        {
            nextAvails[regionIndex] = memoryCached.bitMapIndex;
            numOfAvails[regionIndex] += 1;
            long[] bitMap      = bitMaps[regionIndex];
            int    bitMapIndex = memoryCached.bitMapIndex;
            int    row         = bitMapIndex >>> 6;
            int    col         = bitMapIndex & 63;
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

    class MemoryFetch extends PooledBuffer
    {
        public MemoryFetch(BufferType bufferType)
        {
            super(bufferType);
        }

        public ChunkListNode fetchNode()
        {
            return chunkListNode;
        }

        public long fetchHandle()
        {
            return handle;
        }

        public int fetchOffset()
        {
            return offset;
        }

    }
}
