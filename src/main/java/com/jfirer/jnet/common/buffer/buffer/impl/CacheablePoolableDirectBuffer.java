package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.nio.ByteBuffer;

public class CacheablePoolableDirectBuffer extends PoolableBuffer<ByteBuffer>
{
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
                memory.put(posi, src, srcOffset, len);
            }
            else
            {
                ByteBuffer srcBuffer = buffer.readableByteBuffer();
                memory.put(posi, srcBuffer, srcBuffer.position(), len);
            }
        }
        else
        {
            byte[] src       = (byte[]) buffer.memory();
            int    srcOffset = buffer.offset() + buffer.getReadPosi();
            memory.put(posi, src, srcOffset, len);
        }
        return this;
    }

    @Override
    protected void compact0(int length)
    {
        memory.put(offset, memory, offset + readPosi, length);
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
    public boolean isDirect()
    {
        return true;
    }

    @Override
    protected void put0(int posi, byte value)
    {
        memory.put(posi, value);
    }

    @Override
    protected void put0(byte[] content, int off, int len, int posi)
    {
        memory.put(posi, content, off, len);
    }

    @Override
    protected void putInt0(int posi, int value)
    {
        memory.putInt(posi, value);
    }

    @Override
    protected void putShort0(int posi, short value)
    {
        memory.putShort(posi, value);
    }

    @Override
    protected void putLong0(int posi, long value)
    {
        memory.putLong(posi, value);
    }

    @Override
    protected byte get0(int posi)
    {
        return memory.get(posi);
    }

    @Override
    protected void get0(byte[] dest, int destOff, int len, int posi)
    {
        memory.get(posi, dest, destOff, len);
    }

    @Override
    protected int getInt0(int posi)
    {
        return memory.getInt(posi);
    }

    @Override
    protected short getShort0(int posi)
    {
        return memory.getShort(posi);
    }

    @Override
    protected long getLong0(int posi)
    {
        return memory.getLong(posi);
    }
}
