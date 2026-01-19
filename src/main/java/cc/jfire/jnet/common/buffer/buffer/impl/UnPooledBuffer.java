package cc.jfire.jnet.common.buffer.buffer.impl;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.Bits;
import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.buffer.buffer.RwDelegation;
import cc.jfire.jnet.common.buffer.buffer.impl.rw.HeapRw;
import cc.jfire.jnet.common.buffer.buffer.impl.rw.UnsafeRw;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class UnPooledBuffer implements IoBuffer
{
    protected final BufferType      bufferType;
    protected final RwDelegation    rwDelegation;
    protected       BufferAllocator allocator;
    protected       int             readPosi;
    protected       int             writePosi;
    //当前buffer的实际起点距离分配的内存的起点的偏移量
    protected       int             offset;
    //buffer当前的容量，因为memory是一个共享的区域
    protected       int             bufferCapacity;
    /*实际的内存区域的字段*/
    protected       Object          memory;
    // 当是堆外内存的时候才会有值，0是非法值，不应该使用
    protected       long            nativeAddress;
    protected       int             memoryCapacity;
    protected       int             memoryOffset;
    @Getter
    protected       AtomicInteger   refCnt;
    /*实际的内存区域的字段*/

    public UnPooledBuffer(BufferType bufferType, BufferAllocator allocator)
    {
        this.bufferType = bufferType;
        this.allocator  = allocator;
        switch (bufferType)
        {
            case HEAP -> rwDelegation = HeapRw.INSTANCE;
            case DIRECT, MEMORY -> throw new UnsupportedOperationException();
            case UNSAFE -> rwDelegation = UnsafeRw.INSTANCE;
            default -> throw new IllegalStateException("Unexpected value: " + bufferType);
        }
    }

    protected void init(UnPooledBuffer newBuffer)
    {
        this.memory         = newBuffer.memory;
        this.nativeAddress  = newBuffer.nativeAddress;
        this.memoryCapacity = newBuffer.memoryCapacity;
        this.memoryOffset   = newBuffer.memoryOffset;
        this.bufferCapacity = newBuffer.bufferCapacity;
        this.readPosi       = newBuffer.readPosi;
        this.writePosi      = newBuffer.writePosi;
        this.offset         = newBuffer.offset;
        this.refCnt         = newBuffer.refCnt;
        refCnt.incrementAndGet();
        newBuffer.free();
    }

    protected void init(UnPooledBuffer parent, int oldReadPosi, int length)
    {
        this.memory         = parent.memory;
        this.nativeAddress  = parent.nativeAddress;
        this.memoryCapacity = parent.memoryCapacity;
        this.memoryOffset   = parent.memoryOffset;
        this.bufferCapacity = length;
        readPosi            = 0;
        writePosi           = length;
        offset              = parent.offset + oldReadPosi;
        refCnt              = parent.refCnt;
        refCnt.incrementAndGet();
    }

    public void init(Object memory, long nativeAddress, int memoryCapacity, int memoryOffset, int compensateOffset, int capacity)
    {
        this.memory         = memory;
        this.nativeAddress  = nativeAddress;
        this.memoryCapacity = memoryCapacity;
        this.memoryOffset   = memoryOffset;
        this.bufferCapacity = capacity;
        this.offset         = memoryOffset + compensateOffset;
        readPosi            = writePosi = 0;
    }

    public void initRefCnt()
    {
        if (refCnt != null)
        {
            if (refCnt.get() == 0)
            {
                refCnt.incrementAndGet();
            }
            else
            {
                throw new IllegalStateException();
            }
        }
        else
        {
            refCnt = new AtomicInteger(1);
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
        return bufferCapacity;
    }

    protected int nextWritePosi(int length)
    {
        int oldPosi = writePosi;
        int posi    = oldPosi + length;
        if (posi > bufferCapacity)
        {
            expansionCapacity(posi);
        }
        writePosi = posi;
        return oldPosi;
    }

    /**
     * 扩容到指定容量。
     * <p>
     * 注意：{@code bufferCapacity << 1} 在超大容量（{@code >= 1GB}）下可能发生 int 溢出。
     * 但在网络框架实际使用中，单个缓冲区不会达到如此大的容量，因此该风险可忽略。
     */
    protected void expansionCapacity(int newCapacity)
    {
        newCapacity = Math.max(newCapacity, (bufferCapacity << 1));
        int           oldReadPosi       = readPosi;
        int           oldWritePosi      = writePosi;
        int           oldOffset         = offset;
        int           oldBufferCapacity = bufferCapacity;
        Object        oldMemory         = memory;
        long          oldNativeAddress  = nativeAddress;
        AtomicInteger oldRefCnt         = refCnt;
        allocator.reAllocate(newCapacity, this);
        memoryCopy(oldMemory, oldNativeAddress, oldOffset, this.memory, this.nativeAddress, this.offset, oldBufferCapacity);
        if (oldRefCnt.decrementAndGet() == 0)
        {
            oldRefCnt.incrementAndGet();
            this.refCnt = oldRefCnt;
        }
        else
        {
            this.refCnt = new AtomicInteger(1);
        }
        readPosi  = oldReadPosi;
        writePosi = oldWritePosi;
    }

    protected void memoryCopy(Object src, long srcNativeAddress, int srcOffset, Object desc, long destNativeAddress, int destOffset, int length)
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
        rwDelegation.put0(posi, b, memory, offset, nativeAddress);
        return this;
    }

    void checkWritePosi(int posi, int length)
    {
        int newPosi = posi + length;
        if (newPosi > bufferCapacity)
        {
            expansionCapacity(newPosi);
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
    public IoBuffer putFloat(float f)
    {
        int posi = nextWritePosi(4);
        rwDelegation.putFloat0(posi, f, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putFloat(float f, int posi)
    {
        checkWritePosi(posi, 4);
        rwDelegation.putFloat0(posi, f, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putDouble(double d)
    {
        int posi = nextWritePosi(8);
        rwDelegation.putDouble0(posi, d, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putDouble(double d, int posi)
    {
        checkWritePosi(posi, 8);
        rwDelegation.putDouble0(posi, d, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putShort(short value, int posi)
    {
        checkWritePosi(posi, 2);
        rwDelegation.putShort0(posi, value, memory, offset, nativeAddress);
        return this;
    }

    @Override
    public IoBuffer putLong(long value, int posi)
    {
        checkWritePosi(posi, 8);
        rwDelegation.putLong0(posi, value, memory, offset, nativeAddress);
        return this;
    }

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
        for (int i = 0; i < bufferCapacity; i++)
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
        return bufferCapacity - writePosi;
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        return rwDelegation.writableByteBuffer(memory, offset, nativeAddress, writePosi, bufferCapacity);
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        return rwDelegation.readableByteBuffer(memory, offset, nativeAddress, readPosi, writePosi);
    }

    @Override
    public ByteBuffer readableByteBuffer(int posi)
    {
        if (posi < readPosi || posi >= writePosi)
        {
            throw new IllegalArgumentException("posi超出了允许范围，只能在[" + readPosi + "," + writePosi + ")内,当前请求位置:" + posi);
        }
        return rwDelegation.readableByteBuffer(memory, offset, nativeAddress, readPosi, posi);
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
    public IoBuffer get(byte[] content, int off, int len, int from)
    {
        if (from + len > writePosi)
        {
            throw new IllegalArgumentException("尝试读取的内容过长，当前没有这么多数据");
        }
        rwDelegation.get0(content, off, len, from, memory, offset, nativeAddress);
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
        if (newCapacity <= bufferCapacity)
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
        return rwDelegation.getInt0(posi, memory, offset, nativeAddress);
    }

    @Override
    public float getFloat()
    {
        int posi = nextReadPosi(4);
        return rwDelegation.getFloat0(posi, memory, offset, nativeAddress);
    }

    @Override
    public double getDouble()
    {
        int posi = nextReadPosi(8);
        return rwDelegation.getDouble0(posi, memory, offset, nativeAddress);
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
    public float getFloat(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getFloat0(posi, memory, offset, nativeAddress);
    }

    @Override
    public double getDouble(int posi)
    {
        checkReadPosi(posi, 4);
        return rwDelegation.getDouble0(posi, memory, offset, nativeAddress);
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

    @Override
    public void free()
    {
        freeMemory();
        //接下来的动作是回收壳本身，也就是Buffer对象本身。
        allocator.cycleBufferInstance(this);
    }

    protected void freeMemory()
    {
        if (refCnt.decrementAndGet() == 0)
        {
            /**
             * 只有最后执行真正free动作的人，才可以持有那个refCount对象。
             */
            freeMemory0();
        }
        else
        {
            /**
             * 如果refCnt不是0，意味着这个Buffer没有引用了，但是里面的内存区域是有引用的。因此，只能归还这个壳。
             */
            refCnt = null;
        }
        memory        = null;
        nativeAddress = readPosi = writePosi = memoryCapacity = memoryOffset = bufferCapacity = offset = 0;
    }

    /**
     * 在执行真正的free动作的时候会调用这个方法
     */
    protected void freeMemory0() {}

    @Override
    public int refCount()
    {
        return refCnt.get();
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

    @Override
    public IoBuffer compact()
    {
        if (refCnt.get() == 1)
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
                readPosi  = 0;
                writePosi = length;
            }
        }
        else
        {
            int length = remainRead();
            compactByNewSpace(length);
            readPosi  = 0;
            writePosi = length;
        }
        return this;
    }

    /**
     * 当引用计数大于1时，通过分配新内存空间来执行 compact 操作。
     * <p>
     * 注意：{@code oldOffset + oldReadPosi} 理论上在极大偏移场景下可能发生 int 溢出。
     * 但实际使用中，offset 和 readPosi 的值都源自同一内存区域，其总和不会超过内存区域大小，因此该风险可忽略。
     */
    protected void compactByNewSpace(int length)
    {
        int           oldReadPosi      = readPosi;
        int           oldOffset        = offset;
        Object        oldMemory        = memory;
        long          oldNativeAddress = nativeAddress;
        AtomicInteger oldRefCnt        = refCnt;
        allocator.reAllocate(Math.max(16, length), this);
        if (length == 0)
        {
            ;
        }
        else
        {
            memoryCopy(oldMemory, oldNativeAddress, oldOffset + oldReadPosi, this.memory, this.nativeAddress, this.offset, length);
        }
        if (oldRefCnt.decrementAndGet() == 0)
        {
            oldRefCnt.incrementAndGet();
            this.refCnt = oldRefCnt;
        }
        else
        {
            refCnt = new AtomicInteger(1);
        }
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
    public IoBuffer get(IoBuffer buffer, int len)
    {
        buffer.put(this, len);
        addReadPosi(len);
        return this;
    }

    @Override
    public IoBuffer slice(int length)
    {
        int            oldReadPosi = nextReadPosi(length);
        UnPooledBuffer sliceBuffer = (UnPooledBuffer) allocator.bufferInstance();
        sliceBuffer.init(this, oldReadPosi, length);
        return sliceBuffer;
    }

    @Override
    public String toString()
    {
        return "UnPooledBuffer{" + "capacity=" + bufferCapacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", refCount=" + refCnt.get() + '}';
    }
}
