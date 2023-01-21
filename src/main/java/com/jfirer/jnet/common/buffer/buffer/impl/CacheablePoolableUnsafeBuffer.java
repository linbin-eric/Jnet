package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.nio.ByteBuffer;

public class CacheablePoolableUnsafeBuffer extends CacheablePoolableBuffer<ByteBuffer>
{
    //当direct时才有值
    protected long selfAddress;

    long realAddress(int posi)
    {
        return selfAddress + posi;
    }

    @Override
    public void init(ByteBuffer memory, int capacity, int offset)
    {
        super.init(memory, capacity, offset);
        selfAddress = getNativeAddress(memory) + offset;
    }

    @Override
    public IoBuffer put(IoBuffer buf, int len)
    {
        if (buf.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        switch (buf.bufferType())
        {
            case HEAP ->
            {
                int posi = nextWritePosi(len);
                Bits.copyFromByteArray((byte[]) buf.memory(), buf.offset() + buf.getReadPosi(), realAddress(posi), len);
            }
            case DIRECT, UNSAFE, MEMORY ->
            {
                int            posi   = nextWritePosi(len);
                AbstractBuffer buffer = (AbstractBuffer) buf;
                Bits.copyDirectMemory(buffer.nativeAddress + buffer.offset + buffer.readPosi, realAddress(posi), len);
            }
        }
        return this;
    }

    @Override
    protected void compact0(int length)
    {
        Bits.copyDirectMemory(selfAddress + readPosi, selfAddress, length);
        writePosi = length;
        readPosi = 0;
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        ByteBuffer duplicate = memory.duplicate();
        duplicate.limit(offset + writePosi).position(offset + readPosi);
        return duplicate;
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        ByteBuffer duplicate = memory.duplicate();
        duplicate.limit(offset + capacity).position(offset + writePosi);
        return duplicate;
    }

    @Override
    public final BufferType bufferType()
    {
        return BufferType.UNSAFE;
    }

    @Override
    protected void put0(int posi, byte value)
    {
        Bits.put(realAddress(posi), value);
    }

    @Override
    protected void put0(byte[] content, int off, int len, int posi)
    {
        Bits.copyFromByteArray(content, off, realAddress(posi), len);
    }

    @Override
    protected void putInt0(int posi, int value)
    {
        Bits.putInt(realAddress(posi), value);
    }

    @Override
    protected void putShort0(int posi, short value)
    {
        Bits.putShort(realAddress(posi), value);
    }

    @Override
    protected void putLong0(int posi, long value)
    {
        Bits.putLong(realAddress(posi), value);
    }

    @Override
    protected byte get0(int posi)
    {
        return Bits.get(realAddress(posi));
    }

    @Override
    protected void get0(byte[] content, int off, int len, int posi)
    {
        Bits.copyToArray(realAddress(posi), content, off, len);
    }

    @Override
    protected int getInt0(int posi)
    {
        return Bits.getInt(realAddress(posi));
    }

    @Override
    protected short getShort0(int posi)
    {
        return Bits.getShort(realAddress(posi));
    }

    @Override
    protected long getLong0(int posi)
    {
        return Bits.getLong(realAddress(posi));
    }
}
