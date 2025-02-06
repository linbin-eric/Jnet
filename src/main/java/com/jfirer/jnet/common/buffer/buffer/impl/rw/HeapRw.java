package com.jfirer.jnet.common.buffer.buffer.impl.rw;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.RwDelegation;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;

import java.nio.ByteBuffer;

public class HeapRw implements RwDelegation
{
    public static final HeapRw INSTANCE = new HeapRw();

    @Override
    public void put0(int posi, byte value, Object memory, int offset, long nativeAddress)
    {
        ((byte[]) memory)[offset + posi] = value;
    }

    @Override
    public void put0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        System.arraycopy(content, off, memory, posi + memoryOffset, len);
    }

    @Override
    public void putInt0(int posi, int value, Object memory, int offset, long nativeAddress)
    {
        Bits.putInt((byte[]) memory, offset + posi, value);
    }

    @Override
    public void putShort0(int posi, short value, Object memory, int offset, long nativeAddress)
    {
        Bits.putShort((byte[]) memory, posi + offset, value);
    }

    @Override
    public void putLong0(int posi, long value, Object memory, int offset, long nativeAddress)
    {
        Bits.putLong((byte[]) memory, posi + offset, value);
    }

    @Override
    public byte get0(int posi, Object memory, int offset, long nativeAddress)
    {
        return ((byte[]) memory)[posi + offset];
    }

    @Override
    public void get0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        System.arraycopy(memory, posi + memoryOffset, content, off, len);
    }

    @Override
    public int getInt0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getInt((byte[]) memory, posi + offset);
    }

    @Override
    public short getShort0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getShort((byte[]) memory, posi + offset);
    }

    @Override
    public long getLong0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getLong((byte[]) memory, posi + offset);
    }

    @Override
    public ByteBuffer writableByteBuffer(Object memory, int offset, long nativeAddress, int writePosi, int capacity)
    {
        return ByteBuffer.wrap((byte[]) memory, offset + writePosi, capacity - writePosi);
    }

    @Override
    public ByteBuffer readableByteBuffer(Object memory, int offset, long nativeAddress, int readPosition, int writePosition)
    {
        return ByteBuffer.wrap((byte[]) memory, readPosition + offset, writePosition - readPosition);
    }

    @Override
    public void compact0(Object memory, int offset, long nativeAddress, int readPosition, int length)
    {
        System.arraycopy(memory, offset + readPosition, memory, offset, length);
    }

    @Override
    public void put(Object destMemory, int destOffset, long destNativeAddress, int destPosi, IoBuffer srcBuf, int len)
    {
        switch (srcBuf.bufferType())
        {
            case HEAP -> System.arraycopy(srcBuf.memory(), srcBuf.getReadPosi() + srcBuf.offset(), destMemory, destOffset + destPosi, len);
            case DIRECT, UNSAFE ->
            {
                BasicBuffer buffer = (BasicBuffer) srcBuf;
                Bits.copyToArray(buffer.nativeAddress() + buffer.offset() + buffer.getReadPosi(), (byte[]) destMemory, destOffset + destPosi, len);
            }
        }
    }
}
