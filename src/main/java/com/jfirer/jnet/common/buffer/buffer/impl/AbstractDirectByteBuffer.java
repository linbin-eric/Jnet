package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractDirectByteBuffer extends AbstractBuffer<ByteBuffer>
{
    @Override
    public IoBuffer put(IoBuffer buffer, int len)
    {
        if (buffer.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        int posi = nextWritePosi(len);
        if (buffer.isDirect())
        {
            if (buffer.memory() instanceof ByteBuffer)
            {
                ByteBuffer src       = (ByteBuffer) buffer.memory();
                int        srcOffset = buffer.offset() + buffer.getReadPosi();
                memory.put(realOffset(posi), src, srcOffset, len);
            }
            else
            {
                ByteBuffer srcBuffer = buffer.readableByteBuffer();
                memory.put(realOffset(posi), srcBuffer, srcBuffer.position(), len);
            }
        }
        else
        {
            byte[] src       = (byte[]) buffer.memory();
            int    srcOffset = buffer.offset() + buffer.getReadPosi();
            memory.put(realOffset(posi), src, srcOffset, len);
        }
        return this;
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
            memory.put(offset, memory, offset + readPosi, length);
            writePosi = length;
            readPosi = 0;
        }
        return this;
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
    public boolean isDirect()
    {
        return true;
    }

    int realOffset(int posi)
    {
        return offset + posi;
    }

    @Override
    void put0(int posi, byte value)
    {
        memory.put(realOffset(posi), value);
    }

    @Override
    void put0(byte[] content, int off, int len, int posi)
    {
        memory.put(realOffset(posi), content, off, len);
    }

    @Override
    void putInt0(int posi, int value)
    {
        memory.putInt(realOffset(posi), value);
    }

    @Override
    void putShort0(int posi, short value)
    {
        memory.putShort(realOffset(posi), value);
    }

    @Override
    void putLong0(int posi, long value)
    {
        memory.putLong(realOffset(posi), value);
    }

    @Override
    byte get0(int posi)
    {
        return memory.get(realOffset(posi));
    }

    @Override
    void get0(byte[] dest, int destOff, int len, int posi)
    {
        memory.get(realOffset(posi), dest, destOff, len);
    }

    @Override
    int getInt0(int posi)
    {
        return memory.getInt(realOffset(posi));
    }

    @Override
    short getShort0(int posi)
    {
        return memory.getShort(realOffset(posi));
    }

    @Override
    long getLong0(int posi)
    {
        return memory.getLong(realOffset(posi));
    }
}
