package com.jfireframework.jnet.common.util;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UNSAFE;

import java.util.Arrays;

abstract class PadFor64Bit
{
    // 64长度的缓存行，要进行填充，需要8个byte。
    long p1, p2, p3, p4, p5, p6, p7;

    public static long noHuop(PadFor64Bit instance)
    {
        return instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7;
    }
}

abstract class ProducerIndex extends PadFor64Bit
{
    volatile long producerIndex;
}

abstract class Pad2 extends ProducerIndex
{
    public long p1, p2, p3, p4, p5, p6, p7;

    public static long noHuop(Pad2 instance)
    {
        return instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7;
    }
}

abstract class ComsumerIndex extends Pad2
{
    protected volatile long consumerIndex;
}

abstract class Pad3 extends ComsumerIndex
{
    long p1, p2, p3, p4, p5, p6, p7;

    public static long noHuop(Pad3 instance)
    {
        return instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7;
    }
}

abstract class Core extends Pad3
{
    static final long consumerIndexAddress  = UNSAFE.getFieldOffset("consumerIndex", ComsumerIndex.class);
    static final long producerIndexAddress  = UNSAFE.getFieldOffset("producerIndex", ProducerIndex.class);
    static final long availableBufferOffset = UNSAFE.arrayBaseOffset(new int[0].getClass());
    static final long bufferOffset          = UNSAFE.arrayBaseOffset(Object[].class);
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

    protected final    Object[] buffer;
    protected final    int      mask;
    protected final    int[]    availableBuffers;
    protected final    int      indexShift;
    protected          long     consumerLimit;
    protected volatile long     producerIndexLimit = 0;

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
            buffer = new Object[size];
            availableBuffers = new int[size];
            Arrays.fill(availableBuffers, -1);
        }
        else
        {
            throw new IllegalArgumentException("capacity 无法计算得到其最小的2次方幂");
        }
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
     * 获取下一个可以使用的生产者下标.在不可用时，如果wait参数为false，则直接返回-1.否则执行Thread.yeild等待后继续尝试，直到有可用的为止
     *
     * @return
     */
    long nextProducerIndex(boolean wait)
    {
        long pIndex = producerIndex;
        if (pIndex < producerIndexLimit)
        {
            if (UNSAFE.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
            {
                return pIndex;
            }
        }
        do
        {
            pIndex = producerIndex;
            if (pIndex < producerIndexLimit)
            {
                if (UNSAFE.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
                {
                    return pIndex;
                }
            }
            else
            {
                producerIndexLimit = consumerIndex + mask + 1;
                if (pIndex >= producerIndexLimit)
                {
                    if (wait == false)
                    {
                        // 队列已满
                        return -1;
                    }
                    else
                    {
                        Thread.yield();
                        producerIndexLimit = consumerIndex + mask + 1;
                    }
                }
                else
                {
                    if (UNSAFE.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
                    {
                        return pIndex;
                    }
                }
            }
        } while (true);
    }

    Object get(long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        return UNSAFE.getObject(buffer, address);
    }
}

public abstract class MPSCFixArray<E> extends Core implements FixArray<E>
{

    public MPSCFixArray(int capacity)
    {
        super(capacity);
        try
        {
            for (int i = 0; i < buffer.length; i++)
            {
                buffer[i] = newInstance();
            }
        } catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }

    protected abstract E newInstance();

    @Override
    public boolean isEmpty()
    {
        long consumerIndex = this.consumerIndex;
        long producerIndex = this.producerIndex;
        return consumerIndex == producerIndex;
    }

    @Override
    public long nextOfferIndex()
    {
        return nextProducerIndex(false);
    }

    @Override
    @SuppressWarnings("unchecked")
    public E getSlot(long index)
    {
        return (E) get(index);
    }

    @Override
    public void commit(long index)
    {
        setAvailable(index);
    }

    @Override
    public long nextAvail()
    {
        long cIndex = this.consumerIndex;
        if (cIndex >= consumerLimit)
        {
            consumerLimit = producerIndex;
            if (cIndex >= consumerLimit)
            {
                return -1;
            }
        }
        int  flag    = (int) (cIndex >>> indexShift);
        long address = ((cIndex & mask) << availableBufferScaleShift) + availableBufferOffset;
        if (isAvailable(address, flag) == false)
        {
            while (isAvailable(address, flag) == false)
            {
//                Thread.yield();
                try
                {
                    Thread.sleep(1);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }

        }
        return cIndex;
    }

    @Override
    public void comsumeAvail(long index)
    {
        setConsumerIndexOrdered(index + 1);
    }

    @Override
    public long waitUntilOfferIndexAvail()
    {
        return nextProducerIndex(true);
    }
}
