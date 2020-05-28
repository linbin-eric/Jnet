package com.jfirer.jnet.common.buffer;

import java.nio.ByteBuffer;

public abstract class AbstractDirectBuffer extends AbstractBuffer<ByteBuffer>
{
    @Override
    public IoBuffer put(IoBuffer buf, int len)
    {
        if (buf.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        if (buf.isDirect())
        {
            AbstractBuffer<ByteBuffer> buffer = (AbstractBuffer) buf;
            int                        posi   = nextWritePosi(len);
            Bits.copyDirectMemory(buffer.address + buffer.readPosi, realAddress(posi), len);
        }
        else
        {
            AbstractBuffer<byte[]> buffer = (AbstractBuffer) buf;
            int                    posi   = nextWritePosi(len);
            Bits.copyFromByteArray( buffer.memory, buffer.offset + buffer.readPosi, realAddress(posi), len);
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
            Bits.copyDirectMemory(address + readPosi, address, length);
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

    long realAddress(int posi)
    {
        return address + posi;
    }

    @Override
    void put0(int posi, byte value)
    {
        Bits.put(realAddress(posi), value);
    }

    @Override
    void put0(byte[] content, int off, int len, int posi)
    {
        Bits.copyFromByteArray(content, off, realAddress(posi), len);
    }

    @Override
    void putInt0(int posi, int value)
    {
        Bits.putInt(realAddress(posi), value);
    }

    @Override
    void putShort0(int posi, short value)
    {
        Bits.putShort(realAddress(posi), value);
    }

    @Override
    void putLong0(int posi, long value)
    {
        Bits.putLong(realAddress(posi), value);
    }

    @Override
    byte get0(int posi)
    {
        return Bits.get(realAddress(posi));
    }

    @Override
    void get0(byte[] content, int off, int len, int posi)
    {
        Bits.copyToArray(realAddress(posi), content, off, len);
    }

    @Override
    int getInt0(int posi)
    {
        return Bits.getInt(realAddress(posi));
    }

    @Override
    short getShort0(int posi)
    {
        return Bits.getShort(realAddress(posi));
    }

    @Override
    long getLong0(int posi)
    {
        return Bits.getLong(realAddress(posi));
    }
}
