package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public abstract class AbstractDirectBuffer extends AbstractBuffer<ByteBuffer>
{
    //当direct时才有值
    protected long address;

    @Override
    public void init(ByteBuffer memory, int capacity, int offset)
    {
        super.init(memory, capacity, offset);
        //！！注意，因为扩容的时候仍然是使用memory计算基础地址再加上offset，因此这里虽然计算了最终的address，但是
        //不能将offset的值修改为其他的值，必须保持其原始值！
        address = getAddress(memory) + offset;
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
                Bits.copyDirectMemory(buffer.directMemoryAddress + buffer.offset + buffer.readPosi, realAddress(posi), len);
            }
            else
            {
                MemorySegment segment    = (MemorySegment) buf.memory();
                long          srcAddress = segment.address().toRawLongValue();
                Bits.copyDirectMemory(srcAddress + buf.offset() + buf.getReadPosi(), realAddress(posi), len);
            }
        }
        else
        {
            AbstractHeapBuffer buffer = (AbstractHeapBuffer) buf;
            int                posi   = nextWritePosi(len);
            Bits.copyFromByteArray(buffer.memory, buffer.offset + buffer.readPosi, realAddress(posi), len);
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
