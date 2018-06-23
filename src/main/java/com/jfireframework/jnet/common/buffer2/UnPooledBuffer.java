package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public abstract class UnPooledBuffer<T> implements IoBuffer
{
    protected T   memory;
    protected int readPosi;
    protected int writePosi;
    protected int capacity;
    
    public void init(T memory, int capacity)
    {
        this.memory = memory;
        this.capacity = capacity;
        readPosi = writePosi = 0;
    }
    
    public static UnPooledBuffer<byte[]> newHeapUnPooledBuffer(int size)
    {
        UnPooledBuffer<byte[]> buffer = new UnPooledHeapBuffer();
        buffer.init(new byte[size], size);
        return buffer;
    }
    
    public static UnPooledBuffer<ByteBuffer> newDirectUnPooledBuffer(int size)
    {
        UnPooledBuffer<ByteBuffer> buffer = new UnPooledDirectBuffer();
        buffer.init(ByteBuffer.allocateDirect(size), size);
        return buffer;
    }
    
    int nextWritePosi(int length)
    {
        int writePosi = this.writePosi;
        int newWritePosi = writePosi + length;
        if (newWritePosi > capacity)
        {
            throw new IllegalArgumentException("数组容量:" + capacity + ",当前写入位置:" + writePosi + ",溢出");
        }
        this.writePosi = newWritePosi;
        return writePosi;
    }
    
    int nextReadPosi(int length)
    {
        int readPosi = this.readPosi;
        int newReadPosi = readPosi + length;
        if (newReadPosi > writePosi)
        {
            throw new IllegalArgumentException("当前读取位置为:" + readPosi + ",截止位置为:" + writePosi);
        }
        this.readPosi = newReadPosi;
        return readPosi;
    }
    
    void checkRead(int posi, int length)
    {
        if (posi + length > writePosi)
        {
            throw new IllegalArgumentException("当前读取位置为:" + posi + ",截止位置为:" + writePosi);
        }
    }
    
    void checkWrite(int posi, int length)
    {
        if (posi + length > capacity)
        {
            throw new IllegalArgumentException("数组容量:" + capacity + ",当前写入位置:" + writePosi + ",溢出");
        }
    }
    
    @Override
    public IoBuffer put(IoBuffer buffer)
    {
        return put(buffer, buffer.remainRead());
    }
    
    @Override
    public IoBuffer put(IoBuffer buffer, int len)
    {
        if (buffer.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        if (buffer instanceof UnPooledHeapBuffer)
        {
            put1((UnPooledHeapBuffer) buffer, len);
        }
        else if (buffer instanceof UnPooledDirectBuffer)
        {
            put2((UnPooledDirectBuffer) buffer, len);
        }
        else
        {
            int originReadPosi = buffer.getReadPosi();
            for (int i = 0; i < len; i++)
            {
                put(buffer.get());
            }
            buffer.setReadPosi(originReadPosi);
        }
        return this;
    }
    
    abstract void put1(UnPooledHeapBuffer buffer, int len);
    
    abstract void put2(UnPooledDirectBuffer buffer, int len);
    
    @Override
    public IoBuffer put(byte b)
    {
        int posi = nextWritePosi(1);
        put0(posi, b);
        return this;
    }
    
    @Override
    public IoBuffer put(byte b, int posi)
    {
        checkWrite(posi, 1);
        put0(posi, b);
        return this;
    }
    
    @Override
    public IoBuffer put(byte[] content)
    {
        int length = content.length;
        int writePosi = nextWritePosi(length);
        put0(content, 0, length, writePosi);
        return this;
    }
    
    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        int posi = nextWritePosi(len);
        put0(content, off, len, posi);
        return this;
    }
    
    @Override
    public IoBuffer putInt(int value, int posi)
    {
        checkWrite(posi, 4);
        putInt0(value, posi);
        return this;
    }
    
    @Override
    public IoBuffer putShort(short value, int posi)
    {
        checkWrite(posi, 2);
        putShort0(value, posi);
        return this;
    }
    
    @Override
    public IoBuffer putLong(long value, int posi)
    {
        checkWrite(posi, 8);
        putLong0(value, posi);
        return this;
    }
    
    @Override
    public IoBuffer putInt(int i)
    {
        int posi = nextWritePosi(4);
        putInt0(i, posi);
        return this;
    }
    
    @Override
    public IoBuffer putShort(short s)
    {
        int posi = nextWritePosi(2);
        putShort0(s, posi);
        return this;
    }
    
    @Override
    public IoBuffer putLong(long l)
    {
        int writePosi = nextWritePosi(8);
        putLong0(l, writePosi);
        return this;
    }
    
    @Override
    public int capacity()
    {
        return capacity;
    }
    
    @Override
    public int getReadPosi()
    {
        return readPosi;
    }
    
    @Override
    public void setReadPosi(int readPosi)
    {
        this.readPosi = readPosi;
    }
    
    @Override
    public int getWritePosi()
    {
        return writePosi;
    }
    
    @Override
    public void setWritePosi(int writePosi)
    {
        this.writePosi = writePosi;
    }
    
    @Override
    public IoBuffer clearData()
    {
        readPosi = writePosi = 0;
        return this;
    }
    
    @Override
    public int remainRead()
    {
        return writePosi - readPosi;
    }
    
    @Override
    public void addReadPosi(int add)
    {
        readPosi += add;
    }
    
    @Override
    public void addWritePosi(int add)
    {
        writePosi += add;
    }
    
    @Override
    public byte get()
    {
        int posi = nextReadPosi(1);
        return get0(posi);
    }
    
    @Override
    public byte get(int posi)
    {
        checkRead(posi, posi);
        return get0(posi);
    }
    
    @Override
    public IoBuffer get(byte[] content)
    {
        int length = content.length;
        int posi = nextReadPosi(length);
        get0(content, 0, length, posi);
        return this;
    }
    
    abstract void get0(byte[] content, int off, int length, int posi);
    
    @Override
    public IoBuffer get(byte[] content, int off, int len)
    {
        int posi = nextReadPosi(len);
        get0(content, off, len, posi);
        return this;
    }
    
    @Override
    public int getInt()
    {
        int posi = nextReadPosi(4);
        return getInt0(posi);
    }
    
    @Override
    public short getShort()
    {
        int posi = nextReadPosi(2);
        return getShort0(posi);
    }
    
    @Override
    public long getLong()
    {
        int posi = nextReadPosi(8);
        return getLong0(posi);
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
    public int remainWrite()
    {
        return capacity - writePosi;
    }
    
    abstract byte get0(int posi);
    
    abstract void put0(int posi, byte b);
    
    abstract void put0(byte[] content, int off, int length, int writePosi);
    
    abstract void putInt0(int i, int posi);
    
    abstract void putShort0(short s, int posi);
    
    abstract void putLong0(long l, int posi);
    
    abstract int getInt0(int posi);
    
    abstract short getShort0(int posi);
    
    abstract long getLong0(int posi);
}
