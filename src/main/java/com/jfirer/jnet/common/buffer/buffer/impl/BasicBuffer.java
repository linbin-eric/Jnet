package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.RwDelegation;
import com.jfirer.jnet.common.buffer.buffer.impl.rw.HeapRw;
import com.jfirer.jnet.common.buffer.buffer.impl.rw.UnsafeRw;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.recycler.Recycler;
import lombok.Getter;

import java.nio.ByteBuffer;

public class BasicBuffer implements IoBuffer
{
    public static   Recycler<BasicBuffer> HEAP_POOL       = new Recycler<>(() -> new BasicBuffer(BufferType.HEAP), BasicBuffer::setRecycleHandler);
    public static   Recycler<BasicBuffer> UNSAFE_POOL     = new Recycler<>(() -> new BasicBuffer(BufferType.UNSAFE), BasicBuffer::setRecycleHandler);
    protected final BufferType            bufferType;
    protected final RwDelegation          rwDelegation;
    protected       int                   readPosi;
    protected       int                   writePosi;
    protected       int                   offset;
    protected       int                   capacity;
    protected       RecycleHandler        recycleHandler;
    @Getter
    protected       StorageSegment        storageSegment;
    protected       int                   watchTraceSkip  = 5;
    protected       int                   watchTraceLimit = 3;

    public BasicBuffer(BufferType bufferType)
    {
        this.bufferType = bufferType;
        switch (bufferType)
        {
            case HEAP -> rwDelegation = HeapRw.INSTANCE;
            case DIRECT, MEMORY -> throw new UnsupportedOperationException();
            case UNSAFE -> rwDelegation = UnsafeRw.INSTANCE;
            default -> throw new IllegalStateException("Unexpected value: " + bufferType);
        }
    }

    public void init(StorageSegment storageSegment)
    {
        init(storageSegment, 0, storageSegment.getCapacity());
    }

    public void init(StorageSegment segment, int compensateOffset, int capacity)
    {
        this.storageSegment = segment;
        offset              = segment.getOffset() + compensateOffset;
        this.capacity       = capacity;
        writePosi           = readPosi = 0;
        storageSegment.addRefCount();
    }
//    public void init(Object memory, int capacity, int offset, long nativeAddress)
//    {
//        this.memory = memory;
//        this.capacity = capacity;
//        this.offset = offset;
//        readPosi = writePosi = 0;
//        //如果当前refCount等于0，意味着这是一个初始化的Buffer，不会有竞争可能性。
//        if (refCount == 0)
//        {
//            refCount = 1;
//            watch = leakDetecter.watch(this, 9);
//        }
//        else
//        {
//            //如果refCount大于1，则有可能发生自身在扩容时，从自身slice的buffer进行free操作。从而导致sliceBuffer的free操作的refCount-1操作被这里的赋值操作给覆盖了。所以必须用csv方式进行。
//            add(0);
//        }
//        this.nativeAddress = nativeAddress;
//        if (bufferType != BufferType.HEAP && nativeAddress == 0)
//        {
//            throw new IllegalArgumentException();
//        }
//    }

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
            expansionCapacity(posi);
        }
        writePosi = posi;
        storageSegment.addInvokeTrace(watchTraceSkip, watchTraceLimit);
        return oldPosi;
    }

    protected void expansionCapacity(int newCapacity)
    {
        int oldReadPosi  = readPosi;
        int oldWritePosi = writePosi;
        storageSegment.addInvokeTrace(watchTraceSkip, watchTraceLimit);
        StorageSegment newSegment = storageSegment.makeNewSegment(newCapacity, bufferType);
        memoryCopy(storageSegment.getMemory(), storageSegment.getNativeAddress(), offset, newSegment.getMemory(), newSegment.getNativeAddress(), newSegment.getOffset(), capacity);
        storageSegment.free();
        init(newSegment);
        readPosi  = oldReadPosi;
        writePosi = oldWritePosi;
    }
//    protected StorageSegment makeNewSegment(int newCapacity)
//    {
//        StorageSegment newSegment = StorageSegment.POOL.get();
//        newCapacity = newCapacity > capacity * 2 ? newCapacity : 2 * capacity;
//        switch (bufferType)
//        {
//            case HEAP ->
//            {
//                newSegment.init(new byte[newCapacity], 0, 0, newCapacity);
//            }
//            case DIRECT, MEMORY ->
//            {
//                throw new UnsupportedOperationException();
//            }
//            case UNSAFE ->
//            {
//                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(newCapacity);
//                newSegment.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), 0, newCapacity);
//            }
//        }
//        return newSegment;
//    }

    private void memoryCopy(Object src, long srcNativeAddress, int srcOffset, Object desc, long destNativeAddress, int destOffset, int length)
    {
        switch (bufferType)
        {
            case HEAP -> System.arraycopy(src, srcOffset, desc, destOffset, length);
            case DIRECT, MEMORY -> throw new IllegalArgumentException();
            case UNSAFE -> Bits.copyDirectMemory(srcNativeAddress + srcOffset, destNativeAddress + destOffset, length);
        }
    }

    @Override
    public IoBuffer put(byte b)
    {
        int posi = nextWritePosi(1);
        rwDelegation.put0(posi, b, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    void checkWritePosi(int posi, int length)
    {
        int newPosi = posi + length;
        if (newPosi > capacity)
        {
            expansionCapacity(newPosi);
        }
        storageSegment.addInvokeTrace(watchTraceSkip, watchTraceLimit);
    }

    @Override
    public IoBuffer put(byte b, int posi)
    {
        checkWritePosi(posi, 1);
        rwDelegation.put0(posi, b, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer put(byte[] content)
    {
        int length = content.length;
        int posi   = nextWritePosi(length);
        rwDelegation.put0(content, 0, length, posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        int posi = nextWritePosi(len);
        rwDelegation.put0(content, off, len, posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
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
        rwDelegation.putInt0(posi, i, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer putInt(int value, int posi)
    {
        checkWritePosi(posi, 4);
        rwDelegation.putInt0(posi, value, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer putShort(short value, int posi)
    {
        checkWritePosi(posi, 2);
        rwDelegation.putShort0(posi, value, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer putLong(long value, int posi)
    {
        checkWritePosi(posi, 8);
        rwDelegation.putLong0(posi, value, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer putShort(short s)
    {
        int posi = nextWritePosi(2);
        rwDelegation.putShort0(posi, s, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer putLong(long l)
    {
        int posi = nextWritePosi(8);
        rwDelegation.putLong0(posi, l, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
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
            rwDelegation.put0(i, (byte) 0, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
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
        storageSegment.addInvokeTrace(watchTraceSkip, watchTraceLimit);
        return oldPosi;
    }

    @Override
    public byte get()
    {
        int posi = nextReadPosi(1);
        return rwDelegation.get0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    void checkReadPosi(int posi, int len)
    {
        storageSegment.addInvokeTrace(watchTraceSkip, watchTraceLimit);
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
        return rwDelegation.get0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
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
        return rwDelegation.writableByteBuffer(storageSegment.getMemory(), offset, storageSegment.getNativeAddress(), writePosi, capacity);
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        return rwDelegation.readableByteBuffer(storageSegment.getMemory(), offset, storageSegment.getNativeAddress(), readPosi, writePosi);
    }

    @Override
    public ByteBuffer readableByteBuffer(int posi)
    {
        if (posi <= readPosi || posi > writePosi)
        {
            throw new IllegalArgumentException("posi超出了允许范围，只能在[" + readPosi + "," + writePosi + ")内");
        }
        return rwDelegation.readableByteBuffer(storageSegment.getMemory(), offset, storageSegment.getNativeAddress(), readPosi, posi);
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
        rwDelegation.get0(content, off, len, posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
        return this;
    }

    @Override
    public IoBuffer get(byte[] content, int off, int len, int from)
    {
        if (from + len > writePosi)
        {
            throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
        }
        rwDelegation.get0(content, off, len, from, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
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
        checkWritePosi(writePosi, add);
        writePosi += add;
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
            expansionCapacity(newCapacity);
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
                if (!miss)
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
        return rwDelegation.getInt0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public short getShort()
    {
        int posi = nextReadPosi(2);
        return rwDelegation.getShort0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public long getLong()
    {
        int posi = nextReadPosi(8);
        return rwDelegation.getLong0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public int getInt(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getInt0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public short getShort(int posi)
    {
        checkReadPosi(posi, 2);
        return rwDelegation.getShort0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public long getLong(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getLong0(posi, storageSegment.getMemory(), offset, storageSegment.getNativeAddress());
    }

    @Override
    public void free()
    {
        storageSegment.free();
        storageSegment = null;
        readPosi       = writePosi = offset = capacity = 0;
        recycleHandler.recycle(this);
    }

    @Override
    public int refCount()
    {
        return storageSegment.getRefCount();
    }

    @Override
    public int offset()
    {
        return offset;
    }

    @Override
    public Object memory()
    {
        return storageSegment.getMemory();
    }

    public long nativeAddress()
    {
        return storageSegment.getNativeAddress();
    }

    public void setRecycleHandler(RecycleHandler handler)
    {
        this.recycleHandler = handler;
    }

    @Override
    public IoBuffer compact()
    {
        if (storageSegment.getRefCount() == 1)
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
                rwDelegation.compact0(storageSegment.getMemory(), offset, storageSegment.getNativeAddress(), readPosi, length);
                readPosi  = 0;
                writePosi = length;
            }
        }
        else
        {
            int            length     = remainRead();
            StorageSegment newSegment = storageSegment.makeNewSegment(length == 0 ? 16 : length, bufferType);
            if (length == 0)
            {
                ;
            }
            else
            {
                memoryCopy(storageSegment.getMemory(), storageSegment.getNativeAddress(), offset + readPosi, newSegment.getMemory(), newSegment.getNativeAddress(), newSegment.getOffset(), length);
            }
            storageSegment.free();
            init(newSegment);
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
        rwDelegation.put(storageSegment.getMemory(), offset, storageSegment.getNativeAddress(), posi, buf, len);
        return this;
    }

    @Override
    public IoBuffer get(IoBuffer buffer, int len)
    {
        buffer.put(this, len);
        addReadPosi(len);
        return this;
    }

    @Override
    public IoBuffer slice(int length)
    {
        int         oldReadPosi = nextReadPosi(length);
        BasicBuffer sliceBuffer = null;
        switch (bufferType)
        {
            case HEAP -> sliceBuffer = HEAP_POOL.get();
            case DIRECT, MEMORY -> throw new UnsupportedOperationException();
            case UNSAFE -> sliceBuffer = UNSAFE_POOL.get();
        }
        sliceBuffer.init(storageSegment, offset + oldReadPosi - storageSegment.getOffset(), length);
        sliceBuffer.setWritePosi(length);
        return sliceBuffer;
    }

    @Override
    public String toString()
    {
        return "BasicBuffer{" + "capacity=" + capacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", refCount=" + storageSegment.getRefCount() + '}';
    }
}
