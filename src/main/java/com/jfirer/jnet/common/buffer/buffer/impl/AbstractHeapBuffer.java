package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public abstract class AbstractHeapBuffer extends AbstractBuffer<byte[]>
{
    @Override
    public IoBuffer compact()
    {
        if (readPosi == 0)
        {
            return this;
        }
        System.arraycopy(memory, offset + readPosi, memory, offset, remainRead());
        writePosi -= readPosi;
        readPosi = 0;
        return this;
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
    public boolean isDirect()
    {
        return false;
    }

    int realPosi(int posi)
    {
        return posi + offset;
    }

    @Override
    void put0(int posi, byte value)
    {
        memory[realPosi(posi)] = value;
    }

    @Override
    void put0(byte[] content, int off, int len, int posi)
    {
        System.arraycopy(content, off, memory, realPosi(posi), len);
    }

    @Override
    void putInt0(int posi, int value)
    {
        Bits.putInt(memory, realPosi(posi), value);
    }

    @Override
    void putShort0(int posi, short value)
    {
        Bits.putShort(memory, realPosi(posi), value);
    }

    @Override
    void putLong0(int posi, long value)
    {
        Bits.putLong(memory, realPosi(posi), value);
    }

    @Override
    byte get0(int posi)
    {
        return memory[realPosi(posi)];
    }

    @Override
    void get0(byte[] content, int off, int len, int posi)
    {
        System.arraycopy(memory, realPosi(posi), content, off, len);
    }

    @Override
    int getInt0(int posi)
    {
        return Bits.getInt(memory, realPosi(posi));
    }

    @Override
    short getShort0(int posi)
    {
        return Bits.getShort(memory, realPosi(posi));
    }

    @Override
    long getLong0(int posi)
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
        if (buf.isDirect())
        {
            int posi = nextWritePosi(len);
            if (buf.memory() instanceof ByteBuffer)
            {
                AbstractBuffer buffer = (AbstractBuffer) buf;
                Bits.copyToArray(buffer.directMemoryAddress() + buffer.offset + buffer.readPosi, memory, realPosi(posi), len);
            }
            else
            {
                MemorySegment segment = (MemorySegment) buf.memory();
                MemorySegment.copy(segment, ValueLayout.JAVA_BYTE, buf.offset() + buf.getReadPosi(), memory, realPosi(posi), len);
            }
        }
        else
        {
            AbstractHeapBuffer buffer = (AbstractHeapBuffer) buf;
            int                posi   = nextWritePosi(len);
            System.arraycopy(buffer.memory, buffer.readPosi + buffer.offset, memory, realPosi(posi), len);
        }
        return this;
    }
}
