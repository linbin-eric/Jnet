package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.CacheablePoolableBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class ThreadCache<T>
{
    public static class MemoryCached<T>
    {
        public Arena<T>         arena;
        public ChunkListNode<T> chunkListNode;
        public int              capacity;
        public int              offset;
        public long             handle;
        public int              bitMapIndex;
        int regionIndex;
    }

    static final int                 smallestMask = ~15;
    final        int                 numOfCached;
    private      Arena<T>            arena;
    private      MemoryCached<T>[][] regionCaches;
    private      long[][]            bitMaps;
    private      int[]               numOfAvails;
    private      int[]               nextAvails;

    public ThreadCache(int numOfCached, int maxCachedCapacity, Arena<T> arena)
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

    public void reAllocate(MemoryCached<T> oldMemoryCache, int newReqCapacity, CacheablePoolableBuffer<T> buffer)
    {
        int oldReadPosi  = buffer.getReadPosi();
        int oldWritePosi = buffer.getWritePosi();
        int oldCapacity  = buffer.capacity();
        int oldOffset    = buffer.offset();
        T   oldMemory    = buffer.memory();
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

    public void allocate(int capacity, CacheablePoolableBuffer<T> buffer)
    {
        int size  = normalizeCapacity(capacity);
        int index = MathUtil.log2(size) - 4;
        if (index < regionCaches.length)
        {
            MemoryCached<T>[] regionCach = regionCaches[index];
            long[]            bitMap     = bitMaps[index];
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
                MemoryCached<T> memoryCached = regionCach[bitMapIndex];
                if (memoryCached == null)
                {
                    MemoryFetch memoryFetch = new MemoryFetch();
                    arena.allocate(size, memoryFetch);
                    memoryCached = new MemoryCached<>();
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

    public void free(MemoryCached<T> memoryCached)
    {
        int               regionIndex = memoryCached.regionIndex;
        MemoryCached<T>[] regionCach  = regionCaches[regionIndex];
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

    class MemoryFetch extends PoolableBuffer<T>
    {
        public ChunkListNode<T> fetchNode()
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

        @Override
        public BufferType bufferType()
        {
            Type actualTypeArgument = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            if (actualTypeArgument == ByteBuffer.class)
            {
                return BufferType.DIRECT;
            }
            else
            {
                return BufferType.HEAP;
            }
        }

        @Override
        public IoBuffer put(IoBuffer buffer, int len)
        {
            return null;
        }

        @Override
        public ByteBuffer readableByteBuffer()
        {
            return null;
        }

        @Override
        public ByteBuffer writableByteBuffer()
        {
            return null;
        }

        @Override
        protected void put0(int posi, byte value)
        {
        }

        @Override
        protected void put0(byte[] content, int off, int len, int posi)
        {
        }

        @Override
        protected void putInt0(int posi, int value)
        {
        }

        @Override
        protected void putShort0(int posi, short value)
        {
        }

        @Override
        protected void putLong0(int posi, long value)
        {
        }

        @Override
        protected byte get0(int posi)
        {
            return 0;
        }

        @Override
        protected void get0(byte[] content, int off, int len, int posi)
        {
        }

        @Override
        protected int getInt0(int posi)
        {
            return 0;
        }

        @Override
        protected short getShort0(int posi)
        {
            return 0;
        }

        @Override
        protected long getLong0(int posi)
        {
            return 0;
        }

        @Override
        protected void compact0(int length)
        {
        }
    }
}
