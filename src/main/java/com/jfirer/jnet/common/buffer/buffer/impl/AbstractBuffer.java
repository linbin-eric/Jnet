package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.RwDelegation;
import com.jfirer.jnet.common.buffer.buffer.impl.rw.HeapRw;
import com.jfirer.jnet.common.buffer.buffer.impl.rw.UnsafeRw;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;

public abstract class AbstractBuffer implements IoBuffer
{
    protected          Object                         memory;
    protected          int                            capacity;
    protected          int                            readPosi;
    protected          int                            writePosi;
    protected          int                            offset;
    // 当是堆外内存的时候才会有值，0是非法值，不应该使用
    protected          long                           nativeAddress;
    protected volatile int                            refCount;
    static final       long                           REF_COUNT_OFFSET = UNSAFE.getFieldOffset("refCount", AbstractBuffer.class);
    protected          RecycleHandler<AbstractBuffer> recycleHandler;
    protected final    BufferType                     bufferType;
    protected final    RwDelegation                   rwDelegation;

    protected AbstractBuffer(BufferType bufferType)
    {
        this.bufferType = bufferType;
        switch (bufferType)
        {
            case HEAP -> rwDelegation = HeapRw.INSTANCE;
            case DIRECT, MEMORY -> throw new UnsupportedOperationException();
            case UNSAFE -> rwDelegation = UnsafeRw.INSTANCE;
            default ->
                    throw new IllegalStateException("Unexpected value: " + bufferType);
        }
    }

    public void init(Object memory, int capacity, int offset, long nativeAddress)
    {
        this.memory = memory;
        this.capacity = capacity;
        this.offset = offset;
        readPosi = writePosi = 0;
        //如果当前refCount等于0，意味着这是一个初始化的Buffer，不会有竞争可能性。
        if (refCount == 0)
        {
            refCount = 1;
        }
        else
        {
            //如果refCount大于1，则有可能发生自身在扩容时，从自身slice的buffer进行free操作。从而导致sliceBuffer的free操作的refCount-1操作被这里的赋值操作给覆盖了。所以必须用csv方式进行。
            add(0);
        }
        this.nativeAddress = nativeAddress;
        if (bufferType != BufferType.HEAP && nativeAddress == 0)
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public BufferType bufferType()
    {
        return bufferType;
    }

    @Override
    public int capacity()
    {
        return capacity;
    }

    protected int nextWritePosi(int length)
    {
        int oldPosi = writePosi;
        int posi    = oldPosi + length;
        if (posi > capacity)
        {
            if (refCount == 1)
            {
                reAllocate(posi);
            }
            else
            {
                throw new IllegalStateException("当前存在slice的部分，无法进行扩容");
            }
        }
        writePosi = posi;
        return oldPosi;
    }

    protected abstract void reAllocate(int posi);

    @Override
    public IoBuffer put(byte b)
    {
        int posi = nextWritePosi(1);
        rwDelegation.put0(posi, b, memory, offset, nativeAddress);
        return this;
    }

    void checkWritePosi(int posi, int length)
    {
        int newPosi = posi + length;
        if (newPosi > capacity)
        {
            reAllocate(newPosi);
        }
    }

    @Override
    public IoBuffer put(byte b, int posi)
    {
        checkWritePosi(posi, 1);
        rwDelegation.put0(posi, b, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer put(byte[] content)
    {
        int length = content.length;
        int posi   = nextWritePosi(length);
        rwDelegation.put0(content, 0, length, posi, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        int posi = nextWritePosi(len);
        rwDelegation.put0(content, off, len, posi, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer put(IoBuffer buffer)
    {
        return put(buffer, buffer.remainRead());
    }

    @Override
    public IoBuffer putInt(int i)
    {
        int posi = nextWritePosi(4);
        rwDelegation.putInt0(posi, i, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putInt(int value, int posi)
    {
        checkWritePosi(posi, 4);
        rwDelegation.putInt0(posi, value, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putShort(short value, int posi)
    {
        checkWritePosi(posi, 2);
        rwDelegation.putShort0(posi, value, memory, offset, nativeAddress);
        return this;
    }
//    protected abstract void putShort0(int posi, short value);

    @Override
    public IoBuffer putLong(long value, int posi)
    {
        checkWritePosi(posi, 8);
        rwDelegation.putLong0(posi, value, memory, offset, nativeAddress);
        return this;
    }
//    protected abstract void putLong0(int posi, long value);

    @Override
    public IoBuffer putShort(short s)
    {
        int posi = nextWritePosi(2);
        rwDelegation.putShort0(posi, s, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putLong(long l)
    {
        int posi = nextWritePosi(8);
        rwDelegation.putLong0(posi, l, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public int getReadPosi()
    {
        return readPosi;
    }

    @Override
    public IoBuffer setReadPosi(int readPosi)
    {
        this.readPosi = readPosi;
        return this;
    }

    @Override
    public int getWritePosi()
    {
        return writePosi;
    }

    @Override
    public IoBuffer setWritePosi(int writePosi)
    {
        this.writePosi = writePosi;
        return this;
    }

    @Override
    public IoBuffer clear()
    {
        readPosi = writePosi = 0;
        return this;
    }

    @Override
    public IoBuffer clearAndErasureData()
    {
        for (int i = 0; i < capacity; i++)
        {
            rwDelegation.put0(i, (byte) 0, memory, offset, nativeAddress);
        }
        readPosi = writePosi = 0;
        return this;
    }

    int nextReadPosi(int len)
    {
        int oldPosi = readPosi;
        int newPosi = oldPosi + len;
        if (newPosi > writePosi)
        {
            throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
        }
        readPosi = newPosi;
        return oldPosi;
    }

    @Override
    public byte get()
    {
        int posi = nextReadPosi(1);
        return rwDelegation.get0(posi, memory, offset, nativeAddress);
    }
//    protected abstract byte get0(int posi);

    void checkReadPosi(int posi, int len)
    {
        posi += len;
        if (posi > writePosi)
        {
            throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
        }
    }

    @Override
    public byte get(int posi)
    {
        checkReadPosi(posi, 1);
        return rwDelegation.get0(posi, memory, offset, nativeAddress);
    }

    @Override
    public int remainRead()
    {
        return writePosi - readPosi;
    }

    @Override
    public int remainWrite()
    {
        return capacity - writePosi;
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        return rwDelegation.writableByteBuffer(memory, offset, nativeAddress, writePosi, capacity);
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        return rwDelegation.readableByteBuffer(memory, offset, nativeAddress, readPosi, writePosi);
    }

    @Override
    public IoBuffer get(byte[] content)
    {
        return get(content, 0, content.length);
    }

    @Override
    public IoBuffer get(byte[] content, int off, int len)
    {
        int posi = nextReadPosi(len);
        rwDelegation.get0(content, off, len, posi, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer addReadPosi(int add)
    {
        readPosi += add;
        return this;
    }

    @Override
    public IoBuffer addWritePosi(int add)
    {
        writePosi += add;
        if (writePosi > capacity)
        {
            reAllocate(writePosi);
        }
        return this;
    }

    @Override
    public IoBuffer capacityReadyFor(int newCapacity)
    {
        if (newCapacity <= capacity)
        {
            ;
        }
        else
        {
            reAllocate(newCapacity);
        }
        return this;
    }

    @Override
    public int indexOf(byte[] array)
    {
        for (int i = readPosi; i < writePosi; i++)
        {
            if (get(i) == array[0])
            {
                int length = array.length;
                if (writePosi - i < length)
                {
                    return -1;
                }
                boolean miss = false;
                for (int l = 0; l < length; l++)
                {
                    if (get(i + l) != array[l])
                    {
                        miss = true;
                        break;
                    }
                }
                if (miss == false)
                {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public int getInt()
    {
        int posi = nextReadPosi(4);
        return rwDelegation.getInt0(posi, memory, offset, nativeAddress);
    }

    @Override
    public short getShort()
    {
        int posi = nextReadPosi(2);
        return rwDelegation.getShort0(posi, memory, offset, nativeAddress);
    }

    @Override
    public long getLong()
    {
        int posi = nextReadPosi(8);
        return rwDelegation.getLong0(posi, memory, offset, nativeAddress);
    }

    @Override
    public int getInt(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getInt0(posi, memory, offset, nativeAddress);
    }

    @Override
    public short getShort(int posi)
    {
        checkReadPosi(posi, 2);
        return rwDelegation.getShort0(posi, memory, offset, nativeAddress);
    }

    @Override
    public long getLong(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getLong0(posi, memory, offset, nativeAddress);
    }

    int incrRef()
    {
        return add(1);
    }

    int descRef()
    {
        return add(-1);
    }

    int add(int add)
    {
        int current = refCount;
        int update  = current + add;
        if (update >= 0 && UNSAFE.compareAndSwapInt(this, REF_COUNT_OFFSET, current, update))
        {
            return update;
        }
        do
        {
            current = refCount;
            update = current + add;
            if (update < 0)
            {
                throw new IllegalStateException("free调用的次数超过了被申请的次数");
            }
        }
        while (UNSAFE.compareAndSwapInt(this, REF_COUNT_OFFSET, current, update) == false);
        return update;
    }

    @Override
    public void free()
    {
        int left = descRef();
        if (left > 0)
        {
            return;
        }
        free0(capacity);
        memory = null;
        if (recycleHandler != null)
        {
            recycleHandler.recycle(this);
        }
    }

    protected abstract void free0(int capacity);

    @Override
    public int refCount()
    {
        return refCount;
    }

    @Override
    public int offset()
    {
        return offset;
    }

    @Override
    public Object memory()
    {
        return memory;
    }

    public long nativeAddress()
    {
        return nativeAddress;
    }

    public void setRecycleHandler(RecycleHandler handler)
    {
        this.recycleHandler = handler;
    }

    @Override
    public IoBuffer compact()
    {
        if (readPosi == 0)
        {
            return this;
        }
        int length = remainRead();
        if (length == 0)
        {
            writePosi = readPosi = 0;
        }
        else
        {
            rwDelegation.compact0(memory, offset, nativeAddress, readPosi, length);
            readPosi = 0;
            writePosi = length;
        }
        return this;
    }

    @Override
    public IoBuffer put(IoBuffer buf, int len)
    {
        if (buf.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        int posi = nextWritePosi(len);
        rwDelegation.put(memory, offset, nativeAddress, posi, buf, len);
        return this;
    }

    @Override
    public String toString()
    {
        return "AbstractBuffer{" + "capacity=" + capacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", refCount=" + refCount + '}';
    }
}
