package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.lang.foreign.MemorySegment;
import java.nio.ByteBuffer;

public class CacheablePoolableUnsafeBuffer extends CacheablePoolableBuffer<ByteBuffer>
{
    //当direct时才有值
    protected long selfAddress;

    long realAddress(int posi)
    {
        return selfAddress + posi;
    }
//    @Override
//    public void init(Arena<ByteBuffer> arena, ChunkListNode<ByteBuffer> chunkListNode, int capacity, int offset, long handle)
//    {
//        super.init(arena, chunkListNode, capacity, offset, handle);
//        //！！注意，因为扩容的时候仍然是使用memory计算基础地址再加上offset，因此这里虽然计算了最终的address，但是
//        //不能将offset的值修改为其他的值，必须保持其原始值！
//        selfAddress = getDirectAddress(memory) + offset;
//    }

    @Override
    public void init(ByteBuffer memory, int capacity, int offset)
    {
        super.init(memory, capacity, offset);
        selfAddress = getDirectAddress(memory) + offset;
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
            int posi = nextWritePosi(len);
            Bits.copyFromByteArray((byte[]) buf.memory(), buf.offset() + buf.getReadPosi(), realAddress(posi), len);
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
    public boolean isDirect()
    {
        return true;
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
