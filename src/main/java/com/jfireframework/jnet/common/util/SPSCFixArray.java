package com.jfireframework.jnet.common.util;

import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UNSAFE;

abstract class SPSCCore extends Pad3
{
    
    static final long consumerIndexAddress = UNSAFE.getFieldOffset("consumerIndex", ComsumerIndex.class);
    static final long producerIndexAddress = UNSAFE.getFieldOffset("producerIndex", ProducerIndex.class);
    static final long bufferOffset         = UNSAFE.arrayBaseOffset(Object[].class);
    static final long bufferScaleShift;
    
    static
    {
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
    
    protected final Object[] buffer;
    protected final int      mask;
    protected final int      indexShift;
    protected long           consumerLimit;
    protected long           producerIndexLimit = 0;
    
    SPSCCore(int capacity)
    {
        int size = 1;
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
    
    /**
     * 获取下一个可以使用的生产者下标
     * 
     * @return
     */
    long nextProducerIndex(boolean wait)
    {
        long pLimit = producerIndexLimit;
        long pIndex = producerIndex;
        if (pIndex < pLimit)
        {
            return pIndex;
        }
        else if (pIndex < (pLimit = consumerIndex + mask + 1))
        {
            return pIndex;
        }
        else
        {
            if (wait == false)
            {
                return -1;
            }
            else
            {
                while (pIndex == (pLimit = consumerIndex + mask + 1))
                {
                    Thread.yield();
                }
                return pIndex;
            }
        }
    }
    
    Object get(long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        return UNSAFE.getObject(buffer, address);
    }
    
    void setAvail(long index)
    {
        UNSAFE.putOrderedLong(this, producerIndexAddress, index + 1);
    }
}

public abstract class SPSCFixArray<E> extends SPSCCore implements FixArray<E>
{
    public SPSCFixArray(int capacity)
    {
        super(capacity);
        try
        {
            for (int i = 0; i < buffer.length; i++)
            {
                buffer[i] = newInstance();
            }
        }
        catch (Throwable e)
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
        setAvail(index);
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
