package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.exception.RelocateNotAllowException;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Setter;

import java.nio.ByteBuffer;

public class SliceSupportReAllocateBuffer implements IoBuffer
{
    private PooledBuffer                                 delegation;
    private PooledBufferAllocator                        allocator;
    @Setter
    private RecycleHandler<SliceSupportReAllocateBuffer> handler;

    public void resetDelegation(PooledBuffer delegation, PooledBufferAllocator allocator)
    {
        this.delegation = delegation;
        this.allocator  = allocator;
    }

    @Override
    public int capacity()
    {
        return delegation.capacity();
    }

    @Override
    public IoBuffer put(byte b)
    {
        try
        {
            return delegation.put(b);
        }
        catch (RelocateNotAllowException e)
        {
            allocator.iobu
        }
    }

    @Override
    public IoBuffer put(byte b, int posi)
    {
        return delegation.put(b, posi);
    }

    @Override
    public IoBuffer put(byte[] content)
    {
        return delegation.put(content);
    }

    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        return delegation.put(content, off, len);
    }

    @Override
    public IoBuffer put(IoBuffer buffer)
    {
        return delegation.put(buffer);
    }

    @Override
    public IoBuffer put(IoBuffer buffer, int len)
    {
        return delegation.put(buffer, len);
    }

    @Override
    public IoBuffer get(IoBuffer buffer, int len)
    {
        return delegation.get(buffer, len);
    }

    @Override
    public IoBuffer putInt(int i)
    {
        return delegation.putInt(i);
    }

    @Override
    public IoBuffer putInt(int value, int posi)
    {
        return delegation.putInt(value, posi);
    }

    @Override
    public IoBuffer putShort(short value, int posi)
    {
        return delegation.putShort(value, posi);
    }

    @Override
    public IoBuffer putLong(long value, int posi)
    {
        return delegation.putLong(value, posi);
    }

    @Override
    public IoBuffer putShort(short s)
    {
        return delegation.putShort(s);
    }

    @Override
    public IoBuffer putLong(long l)
    {
        return delegation.putLong(l);
    }

    @Override
    public int getReadPosi()
    {
        return delegation.getReadPosi();
    }

    @Override
    public IoBuffer setReadPosi(int readPosi)
    {
        return delegation.setReadPosi(readPosi);
    }

    @Override
    public int getWritePosi()
    {
        return delegation.getWritePosi();
    }

    @Override
    public IoBuffer setWritePosi(int writePosi)
    {
        return delegation.setWritePosi(writePosi);
    }

    @Override
    public IoBuffer clear()
    {
        return delegation.clear();
    }

    @Override
    public IoBuffer clearAndErasureData()
    {
        return delegation.clearAndErasureData();
    }

    @Override
    public byte get()
    {
        return delegation.get();
    }

    @Override
    public byte get(int posi)
    {
        return delegation.get(posi);
    }

    @Override
    public int remainRead()
    {
        return delegation.remainRead();
    }

    @Override
    public int remainWrite()
    {
        return delegation.remainWrite();
    }

    @Override
    public IoBuffer compact()
    {
        return delegation.compact();
    }

    @Override
    public IoBuffer get(byte[] content)
    {
        return delegation.get(content);
    }

    @Override
    public IoBuffer get(byte[] content, int off, int len)
    {
        return delegation.get(content, off, len);
    }

    @Override
    public IoBuffer get(byte[] content, int off, int len, int from)
    {
        return delegation.get(content, off, len, from);
    }

    @Override
    public IoBuffer addReadPosi(int add)
    {
        return delegation.addReadPosi(add);
    }

    @Override
    public IoBuffer addWritePosi(int add)
    {
        return delegation.addWritePosi(add);
    }

    @Override
    public int indexOf(byte[] array)
    {
        return delegation.indexOf(array);
    }

    @Override
    public int getInt()
    {
        return delegation.getInt();
    }

    @Override
    public short getShort()
    {
        return delegation.getShort();
    }

    @Override
    public long getLong()
    {
        return delegation.getLong();
    }

    @Override
    public int getInt(int posi)
    {
        return delegation.getInt(posi);
    }

    @Override
    public short getShort(int posi)
    {
        return delegation.getShort(posi);
    }

    @Override
    public long getLong(int posi)
    {
        return delegation.getLong(posi);
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        return delegation.readableByteBuffer();
    }

    @Override
    public ByteBuffer readableByteBuffer(int posi)
    {
        return delegation.readableByteBuffer(posi);
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        return delegation.writableByteBuffer();
    }

    @Override
    public BufferType bufferType()
    {
        return delegation.bufferType();
    }

    @Override
    public void free()
    {
        delegation.free();
        delegation = null;
        handler.recycle(this);
    }

    @Override
    public IoBuffer capacityReadyFor(int newCapacity)
    {
        return delegation.capacityReadyFor(newCapacity);
    }

    @Override
    public int refCount()
    {
        return delegation.refCount;
    }

    @Override
    public int offset()
    {
        return delegation.offset();
    }

    @Override
    public Object memory()
    {
        return delegation.memory();
    }
}
