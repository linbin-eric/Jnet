package com.jfireframework.jnet.common.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UnsafeFieldAccess;
import sun.misc.Unsafe;

abstract class Core<E> extends Pad2
{
    protected E[] buffer;
    
    Core(int capacity)
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
            availableBuffers = new int[size];
            Arrays.fill(availableBuffers, -1);
        }
        else
        {
            throw new IllegalArgumentException("capacity 无法计算得到其最小的2次方幂");
        }
    }
    
}

abstract class Pad3<E> extends Core<E>
{
    long p0, p1, p2, p3, p4, p5, p6, p7;
    long p11, p12, p13, p14, p15, p16, p17;
    
    @SuppressWarnings("unchecked")
    Pad3(int capacity)
    {
        super(capacity);
        try
        {
            Type type = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            Class<E> ckass = (Class<E>) type;
            int length = mask + 1;
            buffer = (E[]) new Object[length];
            for (int i = 0; i < length; i++)
            {
                buffer[i] = ckass.newInstance();
            }
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }
    
    public static long noHuop(Pad3<?> instance)
    {
        return instance.p0 + instance.p1 + instance.p2 + instance.p3 + instance.p4 + instance.p5 + instance.p6 + instance.p7 + instance.p11 + instance.p12 + instance.p13 + instance.p14 + instance.p15 + instance.p16 + instance.p17;
    }
}

abstract class ComsumerIndex<E> extends Pad3<E>
{
    long consumerIndex;
    
    ComsumerIndex(int capacity)
    {
        super(capacity);
    }
    
}

abstract class Pad4<E> extends ComsumerIndex<E>
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

abstract class ProducerIndexLimit<E> extends Pad4<E>
{
    volatile long producerIndexLimit = 0;
    
    ProducerIndexLimit(int capacity)
    {
        super(capacity);
    }
}

abstract class Pad5<E> extends ProducerIndexLimit<E>
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

@SuppressWarnings("restriction")
abstract class AccessInfo<E> extends Pad5<E>
{
    
    static Unsafe     unsafe                    = ReflectUtil.getUnsafe();
    static final long consumerIndexAddress      = UnsafeFieldAccess.getFieldOffset("consumerIndex", ComsumerIndex.class);
    static final long producerIndexAddress      = UnsafeFieldAccess.getFieldOffset("producerIndex", ProducerIndex.class);
    static final long producerIndexLimitAddress = UnsafeFieldAccess.getFieldOffset("producerIndexLimit", ProducerIndexLimit.class);
    static final long availableBufferOffset     = unsafe.arrayBaseOffset(new int[0].getClass());
    static final long bufferOffset              = unsafe.arrayBaseOffset(Object[].class);
    static final long availableBufferScaleShift;
    static final long bufferScaleShift;
    
    static
    {
        int availableBufferScale = unsafe.arrayIndexScale(new int[0].getClass());
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
        int bufferScale = unsafe.arrayIndexScale(Object[].class);
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
        return unsafe.getLongVolatile(this, consumerIndexAddress);
    }
    
    final void setConsumerIndexOrdered(long consumerIndex)
    {
        unsafe.putOrderedLong(this, consumerIndexAddress, consumerIndex);
    }
    
    boolean isAvailable(long index)
    {
        int flag = (int) (index >>> indexShift);
        long address = ((index & mask) << availableBufferScaleShift) + availableBufferOffset;
        return unsafe.getIntVolatile(availableBuffers, address) == flag;
    }
    
    boolean isAvailable(long address, int flag)
    {
        return unsafe.getIntVolatile(availableBuffers, address) == flag;
    }
    
    void setAvailable(long index)
    {
        int flag = (int) (index >>> indexShift);
        long address = ((index & mask) << availableBufferScaleShift) + availableBufferOffset;
        unsafe.putOrderedInt(availableBuffers, address, flag);
    }
    
    /**
     * 获取下一个可以使用的生产者下标
     * 
     * @return
     */
    long nextProducerIndex()
    {
        long producerIndexAddress = AccessInfo.producerIndexAddress;
        int mask = this.mask;
        long pLimit = producerIndexLimit;
        long pIndex = producerIndex;
        if (pIndex < pLimit)
        {
            if (unsafe.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
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
                if (unsafe.compareAndSwapLong(this, producerIndexAddress, pIndex, pIndex + 1))
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
    
    @SuppressWarnings("unchecked")
    E get(long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        return (E) unsafe.getObject(buffer, address);
    }
    
    void set(Object value, long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        unsafe.putObject(buffer, address, value);
    }
    
    Object getAndSetNull(long index)
    {
        long address = ((index & mask) << bufferScaleShift) + bufferOffset;
        Object result = unsafe.getObject(buffer, address);
        unsafe.putObject(buffer, address, null);
        return result;
    }
    
    void setProducerIndexLimit(long limit)
    {
        unsafe.putOrderedLong(this, producerIndexLimitAddress, limit);
    }
    
    void waitUnitlAvailable(long index)
    {
        int flag = (int) (index >>> indexShift);
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

public class SPSCFixArray<E> extends AccessInfo<E>
{
    
    public SPSCFixArray(int capacity)
    {
        super(capacity);
    }
    
    /**
     * 获取下一个可以写入的下标。如果当前已经满了，返回-1
     * 
     * @return
     */
    public long nextOfferIndex()
    {
        return nextProducerIndex();
    }
    
    public E getEntry(long index)
    {
        return get(index);
    }
    
    public void commitOfferIndex(long index)
    {
        setAvailable(index);
    }
    
    /**
     * 返回下一个可以读取的坐标，如果返回-1意味着已经没有可以读取的内容了
     * 
     * @return
     */
    public long nextComsumerndex()
    {
        long pIndex = producerIndex;
        long cIndex = this.consumerIndex;
        if (pIndex == cIndex)
        {
            return -1;
        }
        int flag = (int) (cIndex >>> indexShift);
        long address = ((cIndex & mask) << availableBufferScaleShift) + availableBufferOffset;
        if (isAvailable(address, flag) == false)
        {
            while (isAvailable(address, flag) == false)
            {
                Thread.yield();
            }
        }
        return cIndex;
    }
    
    public void consumeIndex(long index)
    {
        setConsumerIndexOrdered(index + 1);
    }
}
