package com.jfirer.jnet.common.buffer.buffer.impl.rw;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.RwDelegation;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;

import java.nio.ByteBuffer;

public class UnsafeRw implements RwDelegation
{
    public static final UnsafeRw INSTANCE = new UnsafeRw();

    @Override
    public void put0(int posi, byte value, Object memory, int offset, long nativeAddress)
    {
        Bits.put(nativeAddress + offset + posi, value);
    }

    @Override
    public void put0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        Bits.copyFromByteArray(content, off, nativeAddress + memoryOffset + posi, len);
    }

    @Override
    public void putInt0(int posi, int value, Object memory, int offset, long nativeAddress)
    {
        Bits.putInt(posi + offset + nativeAddress, value);
    }

    @Override
    public void putShort0(int posi, short value, Object memory, int offset, long nativeAddress)
    {
        Bits.putShort(posi + offset + nativeAddress, value);
    }

    @Override
    public void putLong0(int posi, long value, Object memory, int offset, long nativeAddress)
    {
        Bits.putLong(posi + offset + nativeAddress, value);
    }

    @Override
    public byte get0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.get(posi + offset + nativeAddress);
    }

    @Override
    public void get0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        Bits.copyToArray(posi + memoryOffset + nativeAddress, content, off, len);
    }

    @Override
    public int getInt0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getInt(posi + offset + nativeAddress);
    }

    @Override
    public short getShort0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getShort(posi + offset + nativeAddress);
    }

    @Override
    public long getLong0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getLong(posi + offset + nativeAddress);
    }

    @Override
    public ByteBuffer writableByteBuffer(Object memory, int offset, long nativeAddress, int writePosi, int capacity)
    {
        ByteBuffer duplicate = ((ByteBuffer) memory).duplicate();
        duplicate.limit(offset + capacity).position(offset + writePosi);
        return duplicate;
    }

    @Override
    public ByteBuffer readableByteBuffer(Object memory, int offset, long nativeAddress, int readPosition, int writePosition)
    {
        ByteBuffer duplicate = ((ByteBuffer) memory).duplicate();
        duplicate.limit(offset + writePosition).position(offset + readPosition);
        return duplicate;
    }

    @Override
    public void compact0(Object memory, int offset, long nativeAddress, int readPosition, int length)
    {
        Bits.copyDirectMemory(offset + nativeAddress + readPosition, offset + nativeAddress, length);
    }

    @Override
    public void put(Object memory, int offset, long nativeAddress, int position, IoBuffer buf, int len)
    {
        switch (buf.bufferType())
        {
            case HEAP ->
                    Bits.copyFromByteArray((byte[]) buf.memory(), buf.offset() + buf.getReadPosi(), offset + nativeAddress + position, len);
            case DIRECT, UNSAFE, MEMORY ->
            {
                AbstractBuffer buffer = (AbstractBuffer) buf;
                Bits.copyDirectMemory(buffer.nativeAddress() + buffer.offset() + buffer.getReadPosi(), offset + nativeAddress + position, len);
            }
        }
    }
}
