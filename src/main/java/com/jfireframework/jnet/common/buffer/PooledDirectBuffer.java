package com.jfireframework.jnet.common.buffer;

public class PooledDirectBuffer extends PooledIoBuffer
{
    @Override
    public boolean isDirect()
    {
        return true;
    }
    
    @Override
    protected void _put(int posi, byte b)
    {
        Bits.putBytes(address + addressOffset + posi, b);
    }
    
    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        if (len > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD)
        {
            checkBounds(content, off, len);
            int posi = nextWritePosi(len);
            Bits.copyFromByteArray(content, off, address + addressOffset + posi, len);
            return this;
        }
        else
        {
            return super.put(content, off, len);
        }
        
    }
    
    @Override
    protected void _put(int posi, IoBuffer buffer, int len)
    {
        if (buffer.isDirect())
        {
            AbstractIoBuffer src = (AbstractIoBuffer) buffer;
            Bits.copyDirectMemory(src.address + src.addressOffset + src.readPosi, address + addressOffset + posi, len);
        }
        else
        {
            AbstractIoBuffer src = (AbstractIoBuffer) buffer;
            Bits.copyFromByteArray(src.array, src.arrayOffset + src.readPosi, address + addressOffset + posi, len);
        }
    }
    
    @Override
    protected void _putInt(int posi, int value)
    {
        long a = address + addressOffset + posi;
        Bits.putBytes(a, int3(value));
        Bits.putBytes(a + 1, int2(value));
        Bits.putBytes(a + 2, int1(value));
        Bits.putBytes(a + 3, int0(value));
    }
    
    @Override
    protected void _putShort(int posi, short value)
    {
        long a = address + addressOffset + posi;
        Bits.putBytes(a, short1(value));
        Bits.putBytes(a + 1, short0(value));
    }
    
    @Override
    protected void _putLong(int posi, long value)
    {
        long a = address + addressOffset + posi;
        Bits.putBytes(a, long7(value));
        Bits.putBytes(a + 1, long6(value));
        Bits.putBytes(a + 2, long5(value));
        Bits.putBytes(a + 3, long4(value));
        Bits.putBytes(a + 4, long3(value));
        Bits.putBytes(a + 5, long2(value));
        Bits.putBytes(a + 6, long1(value));
        Bits.putBytes(a + 7, long0(value));
    }
    
    @Override
    protected byte _get(int posi)
    {
        long a = address + addressOffset + posi;
        return Bits.getBytes(a);
    }
    
    @Override
    public IoBuffer compact()
    {
        if (readPosi == 0)
        {
            return this;
        }
        int length = writePosi - readPosi;
        Bits.copyDirectMemory(address + addressOffset + readPosi, address + addressOffset, length);
        writePosi = length;
        readPosi = 0;
        return this;
    }
    
    @Override
    public IoBuffer get(byte[] content, int off, int len)
    {
        if (len > Bits.JNI_COPY_TO_ARRAY_THRESHOLD)
        {
            
            checkBounds(content, off, len);
            if (remainRead() < len)
            {
                throw new IllegalArgumentException();
            }
            int posi = nextReadPosi(len);
            Bits.copyToArray(address + addressOffset + posi, content, off, len);
            return this;
        }
        else
        {
            return super.get(content, off, len);
        }
    }
}
