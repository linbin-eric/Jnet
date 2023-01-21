package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public class CacheablePoolableHeapBuffer extends CacheablePoolableBuffer<byte[]>
{
    @Override
    protected void compact0(int length)
    {
        System.arraycopy(memory, offset + readPosi, memory, offset, length);
        writePosi = length;
        readPosi = 0;
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        return ByteBuffer.wrap(memory, readPosi + offset, remainRead());
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        return ByteBuffer.wrap(memory, offset + writePosi, remainWrite());
    }

    @Override
    public BufferType bufferType()
    {
        return BufferType.HEAP;
    }

    int realPosi(int posi)
    {
        return posi + offset;
    }

    @Override
    protected void put0(int posi, byte value)
    {
        memory[realPosi(posi)] = value;
    }

    @Override
    protected void put0(byte[] content, int off, int len, int posi)
    {
        System.arraycopy(content, off, memory, realPosi(posi), len);
    }

    @Override
    protected void putInt0(int posi, int value)
    {
        Bits.putInt(memory, realPosi(posi), value);
    }

    @Override
    protected void putShort0(int posi, short value)
    {
        Bits.putShort(memory, realPosi(posi), value);
    }

    @Override
    protected void putLong0(int posi, long value)
    {
        Bits.putLong(memory, realPosi(posi), value);
    }

    @Override
    protected byte get0(int posi)
    {
        return memory[realPosi(posi)];
    }

    @Override
    protected void get0(byte[] content, int off, int len, int posi)
    {
        System.arraycopy(memory, realPosi(posi), content, off, len);
    }

    @Override
    protected int getInt0(int posi)
    {
        return Bits.getInt(memory, realPosi(posi));
    }

    @Override
    protected short getShort0(int posi)
    {
        return Bits.getShort(memory, realPosi(posi));
    }

    @Override
    protected long getLong0(int posi)
    {
        return Bits.getLong(memory, realPosi(posi));
    }

    @Override
    public IoBuffer put(IoBuffer buf, int len)
    {
        if (buf.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        int posi = nextWritePosi(len);
        switch (buf.bufferType())
        {
            case HEAP ->
            {
                System.arraycopy(buf.memory(), buf.getReadPosi() + buf.offset(), memory, realPosi(posi), len);
            }
            case DIRECT, UNSAFE ->
            {
                AbstractBuffer buffer = (AbstractBuffer) buf;
                Bits.copyToArray(buffer.nativeAddress() + buffer.offset + buffer.readPosi, memory, realPosi(posi), len);
            }
            case MEMORY ->
            {
                MemorySegment segment = (MemorySegment) buf.memory();
                MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, buf.offset() + buf.getReadPosi(), memory, realPosi(posi), len);
            }
        }
        return this;
    }
}
