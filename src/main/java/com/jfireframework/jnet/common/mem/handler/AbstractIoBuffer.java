package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

public abstract class AbstractIoBuffer<T>
{
    protected T          mem;
    protected int        index;
    protected Chunk<T>   chunk;
    protected ByteBuffer cachedByteBuffer;
    protected int        capacity;
    protected int        readPosi;
    protected int        writePosi;
    protected Archon<T>  archon;
    
    public void initialize(int off, int capacity, T mem, int index, Chunk<T> chunk, Archon<T> archon)
    {
        this.capacity = capacity;
        this.archon = archon;
        this.mem = mem;
        this.index = index;
        this.chunk = chunk;
        cachedByteBuffer = null;
        _initialize(off, capacity, mem, index, chunk, archon);
    }
    
    public abstract void _initialize(int off, int capacity, T mem, int index, Chunk<T> chunk, Archon<T> archon);
    
    /**
     * 拷贝src中的数据到自身中。执行该操作时，自身应该处于初始化的状态。该拷贝方法会复制的信息包含:<br/>
     * 1. 从off到writePosi的所有数据<br/>
     * 2. src的相对readPosi <br/>
     * 3. src的相对writePosi <br/>
     * 
     * @param src
     */
    public abstract void copy(AbstractIoBuffer<T> src);
    
    /**
     * 将src中的内容替换到自身中。该替换方法会替换的信息包含:<br/>
     * 1. mem <br/>
     * 2. chunk <br/>
     * 3. archon <br/>
     * 4. readPosi <br/>
     * 5. writePosi <br/>
     * 6. off <br/>
     * 7. capacity <br/>
     * 8. index <br/>
     * 
     * @param src
     */
    public abstract void replace(AbstractIoBuffer<T> src);
    
    protected void ensureEnoughWrite(int needToWrite)
    {
        if (needToWrite < 0 || remainWrite() >= needToWrite)
        {
            return;
        }
        archon.expansion(this, capacity + needToWrite);
    }
    
    public Chunk<T> belong()
    {
        return chunk;
    }
    
    public void release()
    {
        if (archon != null)
        {
            archon.recycle(this);
        }
    }
    
    public void destory()
    {
        index = -1;
        mem = null;
        chunk = null;
        archon = null;
        cachedByteBuffer = null;
    }
    
    @Override
    public String toString()
    {
        return "Handler [mem=" + mem + ", index=" + index + ", chunk=" + chunk + ", cachedByteBuffer=" + cachedByteBuffer + ", capacity=" + capacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", archon=" + archon + "]";
    }
    
    public int getIndex()
    {
        return index;
    }
    
    public ByteBuffer cachedByteBuffer()
    {
        if (cachedByteBuffer != null)
        {
            return cachedByteBuffer;
        }
        return byteBuffer();
    }
    
    public int capacity()
    {
        return capacity;
    }
    
    public AbstractIoBuffer<T> put(byte b)
    {
        ensureEnoughWrite(1);
        _put(b);
        return this;
    }
    
    protected abstract void _put(byte b);
    
    public AbstractIoBuffer<T> put(byte b, int posi)
    {
        if (posi < 0)
        {
            throw new IllegalArgumentException();
        }
        ensureEnoughWrite(posi - getWritePosi());
        _put(b, posi);
        return this;
    }
    
    protected abstract void _put(byte b, int posi);
    
    public AbstractIoBuffer<T> put(byte[] content)
    {
        ensureEnoughWrite(content.length);
        _put(content);
        return this;
    }
    
    protected abstract void _put(byte[] content);
    
    public AbstractIoBuffer<T> put(byte[] content, int off, int len)
    {
        ensureEnoughWrite(off + len - getWritePosi());
        _put(content, off, len);
        return this;
    }
    
    protected abstract void _put(byte[] content, int off, int len);
    
    public AbstractIoBuffer<T> put(IoBuffer<?> bucket)
    {
        return put(bucket, bucket.remainRead());
    }
    
    public AbstractIoBuffer<T> put(IoBuffer<?> handler, int len)
    {
        ensureEnoughWrite(len);
        _put(handler, len);
        return this;
    }
    
    protected abstract void _put(IoBuffer<?> handler, int len);
    
    public AbstractIoBuffer<T> writeInt(int i, int off)
    {
        ensureEnoughWrite(off + 4 - getWritePosi());
        _writeInt(i, off);
        return this;
    }
    
    protected abstract void _writeInt(int i, int off);
    
    public AbstractIoBuffer<T> writeShort(short s, int off)
    {
        ensureEnoughWrite(off + 2 - getWritePosi());
        _writeShort(s, off);
        return this;
    }
    
    protected abstract void _writeShort(short s, int off);
    
    public IoBuffer<T> writeLong(long l, int off)
    {
        ensureEnoughWrite(off + 8 - getWritePosi());
        _writeLong(l, off);
        return this;
    }
    
    protected abstract void _writeLong(long l, int off);
    
    public IoBuffer<T> writeInt(int i)
    {
        ensureEnoughWrite(4);
        _writeInt(i);
        return this;
    }
    
    protected abstract void _writeInt(int i);
    
    public AbstractIoBuffer<T> writeShort(short s)
    {
        ensureEnoughWrite(2);
        _writeShort(s);
        return this;
    }
    
    protected abstract void _writeShort(short s);
    
    public AbstractIoBuffer<T> writeLong(long l)
    {
        ensureEnoughWrite(8);
        _writeLong(l);
        return this;
    }
    
    protected abstract void _writeLong(long l);
    
    public abstract int getReadPosi();
    
    public abstract void setReadPosi(int readPosi);
    
    public abstract int getWritePosi();
    
    public abstract void setWritePosi(int writePosi);
    
    public abstract IoBuffer<T> clearData();
    
    public abstract byte get();
    
    public abstract byte get(int posi);
    
    public abstract int remainRead();
    
    public abstract int remainWrite();
    
    public abstract IoBuffer<T> compact();
    
    public abstract IoBuffer<T> get(byte[] content);
    
    public abstract IoBuffer<T> get(byte[] content, int off, int len);
    
    public abstract void addReadPosi(int add);
    
    public abstract void addWritePosi(int add);
    
    public abstract int indexOf(byte[] array);
    
    public abstract int readInt();
    
    public abstract short readShort();
    
    public abstract long readLong();
    
    public abstract ByteBuffer byteBuffer();
    
}
