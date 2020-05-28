package com.jfirer.jnet.common.buffer;


import com.jfirer.jnet.common.util.UNSAFE;

import java.util.Arrays;

class Entry<T>
{
    Chunk<T> chunk;
    long     handle;
}

abstract class PadFor128Bit
{
    // 128长度的缓存行，要进行填充，需要16个byte。
    long p0, p1, p2, p3, p4, p5, p6, p7;
    long p11, p12, p13, p14, p15, p16, p17;

    public static long noHuop(PadFor128Bit instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class ProducerIndex extends PadFor128Bit
{
    volatile long producerIndex;
}

abstract class Pad2 extends ProducerIndex
{
    public long p0, p1, p2, p3, p4, p5, p6, p7;
    public long p11, p12, p13, p14, p15, p16, p17;

    public static long noHuop(Pad2 instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class Core<T> extends Pad2
{
    protected final Entry<T>[] entries;
    protected final int        mask;
    protected final int[]      availableBuffers;
    protected final int        indexShift;

    @SuppressWarnings({"rawtypes", "unchecked"})
    Core(int capacity)
    {
        int size       = 1;
        int indexShift = 0;
        while (size < capacity && size > 0)
        {
            size <<= 1;
            indexShift++;
        }
        if (size > 0)
        {
            this.indexShift = indexShift;
            mask = size - 1;
            entries = new Entry[size];
            for (int i = 0; i < entries.length; i++)
            {
                entries[i] = new Entry();
            }
            availableBuffers = new int[size];
            Arrays.fill(availableBuffers, -1);
        }
        else
        {
            throw new IllegalArgumentException("capacity 无法计算得到其最小的2次方幂");
        }
    }
}

abstract class Pad3<T> extends Core<T>
{
    long p0, p1, p2, p3, p4, p5, p6, p7;
    long p11, p12, p13, p14, p15, p16, p17;

    Pad3(int capacity)
    {
        super(capacity);
    }

    public static long noHuop(Pad3<?> instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class ComsumerIndex<T> extends Pad3<T>
{
    long consumerIndex;

    ComsumerIndex(int capacity)
    {
        super(capacity);
    }
}

abstract class Pad4<T> extends ComsumerIndex<T>
{
    long p0, p1, p2, p3, p4, p5, p6, p7;
    long p11, p12, p13, p14, p15, p16, p17;

    Pad4(int capacity)
    {
        super(capacity);
    }

    public static long noHuop(Pad4<?> instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class ProducerIndexLimit<T> extends Pad4<T>
{
    volatile long producerIndexLimit = 0;

    ProducerIndexLimit(int capacity)
    {
        super(capacity);
    }
}

abstract class Pad5<T> extends ProducerIndexLimit<T>
{
    long p0, p1, p2, p3, p4, p5, p6, p7;
    long p11, p12, p13, p14, p15, p16, p17;

    Pad5(int capacity)
    {
        super(capacity);
    }

    public static long noHuop(Pad5<?> instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class AccessInfo<T> extends Pad5<T>
{

    static final long consumerIndexAddress      = UNSAFE.getFieldOffset("consumerIndex", ComsumerIndex.class);
    static final long producerIndexAddress      = UNSAFE.getFieldOffset("producerIndex", ProducerIndex.class);
    static final long producerIndexLimitAddress = UNSAFE.getFieldOffset("producerIndexLimit", ProducerIndexLimit.class);
    static final long availableBufferOffset     = UNSAFE.arrayBaseOffset(new int[0].getClass());
    static final long bufferOffset              = UNSAFE.arrayBaseOffset(Entry[].class);
    static final long availableBufferScaleShift;
    static final long bufferScaleShift;

    static
    {
        int availableBufferScale = UNSAFE.arrayIndexScale(new int[0].getClass());
        if (availableBufferScale == 4)
        {
            availableBufferScaleShift = 2;
        }
        else if (availableBufferScale == 8)
        {
            availableBufferScaleShift = 3;
        }
        else
        {
            throw new IllegalArgumentException();
        }
        int bufferScale = UNSAFE.arrayIndexScale(Object[].class);
        if (bufferScale == 4)
        {
            bufferScaleShift = 2;
        }
        else if (bufferScale == 8)
        {
            bufferScaleShift = 3;
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    AccessInfo(int capacity)
    {
        super(capacity);
    }

    final long getConsumerIndexVolatile()
    {
        return UNSAFE.getLongVolatile(this, consumerIndexAddress);
    }

    final void setConsumerIndexOrdered(long consumerIndex)
    {
        UNSAFE.putOrderedLong(this, consumerIndexAddress, consumerIndex);
    }

    boolean isAvailable(long index)
    {
        int  flag    = (int) (index >>> indexShift);
        long address = ((index & mask) << availableBufferScaleShift) + availableBufferOffset;
        return UNSAFE.getIntVolatile(availableBuffers, address) == flag;
    }

    boolean isAvailable(long address, int flag)
    {
        return UNSAFE.getIntVolatile(availableBuffers, address) == flag;
    }

    void setAvailable(long index)
    {
        int  flag    = (int) (index >>> indexShift);
        long address = ((index & mask) << availableBufferScaleShift) + availableBufferOffset;
        UNSAFE.putOrderedInt(availableBuffers, address, flag);
    }

    /**
     * 获取下一个可以使用的生产者下标
     *
     * @return
     */
    long nextProducerIndex()
    {
        long producerIndexAddress = AccessInfo.producerIndexAddress;
        int  mask                 = this.mask;
        long pLimit               = producerIndexLimit;
        long pIndex               = producerIndex;
        if (pIndex < pLimit)
        {
            if (UNSAFE.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
            {
                return pIndex;
            }
        }
        boolean limitChange = false;
        do
        {
            pIndex = producerIndex;
            if (pIndex < pLimit)
            {
                if (UNSAFE.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
                {
                    if (limitChange)
                    {
                        setProducerIndexLimit(pLimit);
                    }
                    return pIndex;
                }
                pIndex = producerIndex;
                if (pIndex >= pLimit)
                {
                    pLimit = getConsumerIndexVolatile() + mask + 1;
                    limitChange = true;
                    if (pIndex >= pLimit)
                    {
                        // 队列已满
                        return -1;
                    }
                }
            }
            else
            {
                pLimit = getConsumerIndexVolatile() + mask + 1;
                if (pIndex >= pLimit)
                {
                    // 队列已满
                    return -1;
                }
                else
                {
                    limitChange = true;
                }
            }
        } while (true);
    }

    Object get(long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        return UNSAFE.getObject(entries, address);
    }

    @SuppressWarnings("unchecked")
    void set(long index, Chunk<T> chunk, long handle)
    {
        long     address = ((index & mask) << bufferScaleShift) + bufferOffset;
        Entry<T> entry   = (Entry<T>) UNSAFE.getObject(entries, address);
        entry.chunk = chunk;
        entry.handle = handle;
    }

    @SuppressWarnings("unchecked")
    void initBuffer(long index, PooledBuffer<T> buffer, ThreadCache cache)
    {
        long     address = ((index & mask) << bufferScaleShift) + bufferOffset;
        Entry<T> entry   = (Entry<T>) UNSAFE.getObject(entries, address);
        Chunk<T> chunk   = entry.chunk;
        long     handle  = entry.handle;
        chunk.initBuf(handle, buffer, cache);
        entry.chunk = null;
    }

    void setProducerIndexLimit(long limit)
    {
        UNSAFE.putOrderedLong(this, producerIndexLimitAddress, limit);
    }

    void waitUnitlAvailable(long index)
    {
        int  flag    = (int) (index >>> indexShift);
        long address = ((index & mask) << availableBufferScaleShift) + availableBufferOffset;
        if (isAvailable(address, flag) == false)
        {
            while (isAvailable(address, flag) == false)
            {
                Thread.yield();
            }
        }
    }
}

public class MemoryRegionCache<T> extends AccessInfo<T>
{
    MemoryRegionCache(int capacity)
    {
        super(capacity);
    }

    boolean offer(Chunk<T> chunk, long handle)
    {
        long index = nextProducerIndex();
        if (index == -1)
        {
            return false;
        }
        set(index, chunk, handle);
        setAvailable(index);
        return true;
    }

    int size()
    {
        long pIndex = producerIndex;
        long cIndex = this.consumerIndex;
        return (int) (pIndex - cIndex);
    }

    boolean isEmpty()
    {
        long pIndex = producerIndex;
        long cIndex = this.consumerIndex;
        return cIndex == pIndex;
    }

    /**
     * 返回true意味着有数据可以使用
     *
     * @param buffer
     * @return
     */
    boolean tryFindAndInitBuffer(PooledBuffer<T> buffer, ThreadCache cache)
    {
        long pIndex = producerIndex;
        long cIndex = this.consumerIndex;
        if (pIndex == cIndex)
        {
            return false;
        }
        int  flag    = (int) (cIndex >>> indexShift);
        long address = ((cIndex & mask) << availableBufferScaleShift) + availableBufferOffset;
        if (isAvailable(address, flag) == false)
        {
            while (isAvailable(address, flag) == false)
            {
                Thread.yield();
            }
        }
        initBuffer(cIndex, buffer, cache);
        setConsumerIndexOrdered(cIndex + 1);
        return true;
    }
}
