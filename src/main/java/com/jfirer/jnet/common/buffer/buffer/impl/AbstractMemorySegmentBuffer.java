package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;

public abstract class AbstractMemorySegmentBuffer extends AbstractBuffer<MemorySegment>
{
    @Override
    public void init(MemorySegment memory, int capacity, int offset)
    {
        super.init(memory, capacity, offset);
    }

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
                ByteBuffer    byteBuffer = buffer.readableByteBuffer();
                MemorySegment srcSegment = MemorySegment.ofBuffer(byteBuffer);
                MemorySegment.copy(srcSegment, byteBuffer.position(), memory, realOffset(posi), len);
            }
            else
            {
                MemorySegment srcSegment = (MemorySegment) buffer.memory();
                MemorySegment.copy(srcSegment, buffer.offset() + buffer.getReadPosi(), memory, realOffset(posi), len);
            }
        }
        else
        {
            byte[]        src        = (byte[]) buffer.memory();
            MemorySegment srcSegment = MemorySegment.ofArray(src);
            MemorySegment.copy(srcSegment, buffer.offset() + buffer.getReadPosi(), memory, realOffset(posi), len);
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
            MemorySegment.copy(memory, offset + readPosi, memory, offset, length);
            writePosi = length;
            readPosi = 0;
        }
        return this;
    }

    @Override
    public ByteBuffer readableByteBuffer()
    {
        ByteBuffer duplicate = memory.asByteBuffer();
        duplicate.limit(offset + writePosi).position(offset + readPosi);
        return duplicate;
    }

    @Override
    public ByteBuffer writableByteBuffer()
    {
        ByteBuffer duplicate = memory.asByteBuffer();
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
        memory.set(ValueLayout.JAVA_BYTE, realOffset(posi), value);
    }

    @Override
    void put0(byte[] content, int off, int len, int posi)
    {
        MemorySegment.copy(content, off, memory, ValueLayout.JAVA_BYTE, realOffset(posi), len);
    }

    @Override
    void putInt0(int posi, int value)
    {
        long realOffset = realOffset(posi);
        memory.set(ValueLayout.JAVA_BYTE, realOffset, (byte) ((value >> 24) & 0xFF));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 1, (byte) ((value >> 16) & 0xFF));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 2, (byte) ((value >> 8) & 0xFF));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 3, (byte) ((value >> 0) & 0xFF));
    }

    @Override
    void putShort0(int posi, short value)
    {
        long realOffset = realOffset(posi);
        memory.set(ValueLayout.JAVA_BYTE, realOffset, (byte) ((value >> 8) & 0xFF));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 1, (byte) ((value >> 0) & 0xFF));
    }

    @Override
    void putLong0(int posi, long value)
    {
        long realOffset = realOffset(posi);
        memory.set(ValueLayout.JAVA_BYTE, realOffset, (byte) (value >> 56));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 1, (byte) ((value >> 48) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 2, (byte) ((value >> 40) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 3, (byte) ((value >> 32) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 4, (byte) ((value >> 24) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 5, (byte) ((value >> 16) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 6, (byte) ((value >> 8) & 0xFFL));
        memory.set(ValueLayout.JAVA_BYTE, realOffset + 7, (byte) (value >> 0));
    }

    @Override
    byte get0(int posi)
    {
        return memory.get(ValueLayout.JAVA_BYTE, realOffset(posi));
    }

    @Override
    void get0(byte[] dest, int destOff, int len, int posi)
    {
        MemorySegment.copy(memory, ValueLayout.JAVA_BYTE, realOffset(posi), dest, destOff, len);
    }

    @Override
    int getInt0(int posi)
    {
        int  realOffset = realOffset(posi);
        byte b0         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 0);
        byte b1         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 1);
        byte b2         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 2);
        byte b3         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 3);
        return ((b0 & 0xFF) << 24) | ((b1 & 0xFF) << 16) | ((b2 & 0xFF) << 8) | ((b3 & 0xFF) << 0);
    }

    @Override
    short getShort0(int posi)
    {
        int  realOffset = realOffset(posi);
        byte b0         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 0);
        byte b1         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 1);
        return (short) (((b0 & 0xFF) << 8) | ((b1 & 0xFF) << 0));
    }

    @Override
    long getLong0(int posi)
    {
        int  realOffset = realOffset(posi);
        byte b0         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 0);
        byte b1         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 1);
        byte b2         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 2);
        byte b3         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 3);
        byte b4         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 4);
        byte b5         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 5);
        byte b6         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 6);
        byte b7         = memory.get(ValueLayout.JAVA_BYTE, realOffset + 7);
        return ((b0 & 0xFFL) << 56) | ((b1 & 0xFFL) << 48) | ((b2 & 0xFFL) << 40) | ((b3 & 0xFFL) << 32) | ((b4 & 0xFFL) << 24) | ((b5 & 0xFFL) << 16) | ((b6 & 0xFFL) << 8) | ((b7 & 0xFFL) << 0);
    }
}
