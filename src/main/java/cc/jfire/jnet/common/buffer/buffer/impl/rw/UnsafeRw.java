package cc.jfire.jnet.common.buffer.buffer.impl.rw;

import cc.jfire.jnet.common.buffer.buffer.Bits;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.buffer.buffer.RwDelegation;
import cc.jfire.jnet.common.buffer.buffer.impl.UnPooledBuffer;

import java.nio.ByteBuffer;

public class UnsafeRw implements RwDelegation
{
    public static final UnsafeRw INSTANCE = new UnsafeRw();

    @Override
    public void put0(int posi, byte value, Object memory, int offset, long nativeAddress)
    {
        Bits.put(nativeAddress + (long)offset + (long)posi, value);
    }

    @Override
    public void put0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        Bits.copyFromByteArray(content, off, nativeAddress + (long)memoryOffset + (long)posi, len);
    }

    @Override
    public void putInt0(int posi, int value, Object memory, int offset, long nativeAddress)
    {
        Bits.putInt(nativeAddress + (long)offset + (long)posi, value);
    }

    @Override
    public void putFloat0(int posi, float value, Object memory, int offset, long nativeAddress)
    {
        int i = Float.floatToRawIntBits(value);
        putInt0(posi, i, memory, offset, nativeAddress);
    }

    @Override
    public void putShort0(int posi, short value, Object memory, int offset, long nativeAddress)
    {
        Bits.putShort(nativeAddress + (long)offset + (long)posi, value);
    }

    @Override
    public void putLong0(int posi, long value, Object memory, int offset, long nativeAddress)
    {
        Bits.putLong(nativeAddress + (long)offset + (long)posi, value);
    }

    @Override
    public void putDouble0(int posi, double value, Object memory, int offset, long nativeAddress)
    {
        long l = Double.doubleToRawLongBits(value);
        putLong0(posi, l, memory, offset, nativeAddress);
    }

    @Override
    public byte get0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.get(nativeAddress + (long)offset + (long)posi);
    }

    @Override
    public void get0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress)
    {
        Bits.copyToArray(nativeAddress + (long)memoryOffset + (long)posi, content, off, len);
    }

    @Override
    public int getInt0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getInt(nativeAddress + (long)offset + (long)posi);
    }

    @Override
    public float getFloat0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Float.intBitsToFloat(getInt0(posi, memory, offset, nativeAddress));
    }

    @Override
    public short getShort0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getShort(nativeAddress + (long)offset + (long)posi);
    }

    @Override
    public long getLong0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Bits.getLong(nativeAddress + (long)offset + (long)posi);
    }

    @Override
    public double getDouble0(int posi, Object memory, int offset, long nativeAddress)
    {
        return Double.longBitsToDouble(getLong0(posi, memory, offset, nativeAddress));
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
        Bits.copyDirectMemory(nativeAddress + (long)offset + (long)readPosition, nativeAddress + (long)offset, length);
    }

    @Override
    public void put(Object destMemory, int destOffset, long destNativeAddress, int destPosi, IoBuffer srcBuf, int len)
    {
        switch (srcBuf.bufferType())
        {
            case HEAP -> Bits.copyFromByteArray((byte[]) srcBuf.memory(), srcBuf.offset() + srcBuf.getReadPosi(), destNativeAddress + (long)destOffset + (long)destPosi, len);
            case DIRECT, UNSAFE, MEMORY ->
            {
                UnPooledBuffer buffer = (UnPooledBuffer) srcBuf;
                Bits.copyDirectMemory(buffer.nativeAddress() + (long)buffer.offset() + (long)buffer.getReadPosi(), destNativeAddress + (long)destOffset + (long)destPosi, len);
            }
        }
    }
}
