package com.jfirer.jnet.common.buffer.buffer.impl.rw;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.RwDelegation;

import java.nio.ByteBuffer;

public class DirectBufferRw implements RwDelegation
{
    @Override
    public void put0(int posi, byte value, Object memory, int offset, long nativeAddress)
    {
    }

    @Override
    public void put0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
    }

    @Override
    public void putInt0(int posi, int value, Object memory, int offset, long nativeAddress)
    {
    }

    @Override
    public void putShort0(int posi, short value, Object memory, int offset, long nativeAddress)
    {
    }

    @Override
    public void putLong0(int posi, long value, Object memory, int offset, long nativeAddress)
    {
    }

    @Override
    public byte get0(int posi, Object memory, int offset, long nativeAddress)
    {
        return 0;
    }

    @Override
    public void get0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
    }

    @Override
    public int getInt0(int posi, Object memory, int offset, long nativeAddress)
    {
        return 0;
    }

    @Override
    public short getShort0(int posi, Object memory, int offset, long nativeAddress)
    {
        return 0;
    }

    @Override
    public long getLong0(int posi, Object memory, int offset, long nativeAddress)
    {
        return 0;
    }

    @Override
    public ByteBuffer writableByteBuffer(Object memory, int offset, long nativeAddress, int writePosi, int capacity)
    {
        return null;
    }

    @Override
    public ByteBuffer readableByteBuffer(Object memory, int offset, long nativeAddress, int readPosition, int writePosition)
    {
        return null;
    }

    @Override
    public void compact0(Object memory, int offset, long nativeAddress, int readPosition, int length)
    {
    }

    @Override
    public void put(Object destMemory, int destOffset, long destNativeAddress, int destPosi, IoBuffer srcBuf, int len)
    {
        switch (srcBuf.bufferType())
        {
            case HEAP ->
            {
                byte[] src       = (byte[]) srcBuf.memory();
                int    srcOffset = srcBuf.offset() + srcBuf.getReadPosi();
                ((ByteBuffer) destMemory).put(destPosi, src, srcOffset, len);
            }
            case DIRECT, UNSAFE ->
            {
                ByteBuffer src       = (ByteBuffer) srcBuf.memory();
                int        srcOffset = srcBuf.offset() + srcBuf.getReadPosi();
                ((ByteBuffer) destMemory).put(destPosi, src, srcOffset, len);
            }
//            case MEMORY ->
//            {
//                ByteBuffer srcBuffer = buffer.readableByteBuffer();
//                destMemory.put(posi, srcBuffer, srcBuffer.destPosi(), len);
//            }
        }
    }
}
