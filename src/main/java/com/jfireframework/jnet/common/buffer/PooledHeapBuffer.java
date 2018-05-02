package com.jfireframework.jnet.common.buffer;

public class PooledHeapBuffer extends PooledIoBuffer
{
    @Override
    public boolean isDirect()
    {
        return false;
    }
    
    @Override
    protected void _put(int posi, byte b)
    {
        array[arrayOffset + posi] = b;
    }
    
    @Override
    public IoBuffer put(byte[] content, int off, int len)
    {
        checkBounds(content, off, len);
        int posi = nextWritePosi(len);
        System.arraycopy(content, off, array, arrayOffset + posi, len);
        return this;
    }
    
    @Override
    public void _put(int posi, IoBuffer buffer, int len)
    {
        if (buffer.isDirect())
        {
            AbstractIoBuffer src = (AbstractIoBuffer) buffer;
            Bits.copyToArray(src.address + src.addressOffset + src.readPosi, array, arrayOffset + posi, len);
        }
        else
        {
            AbstractIoBuffer src = (AbstractIoBuffer) buffer;
            System.arraycopy(src.array, src.arrayOffset + src.readPosi, array, posi, len);
        }
    }
    
    @Override
    protected void _putInt(int posi, int value)
    {
        posi = arrayOffset + posi;
        array[posi] = int3(value);
        array[posi + 1] = int2(value);
        array[posi + 2] = int1(value);
        array[posi + 3] = int0(value);
    }
    
    @Override
    protected void _putShort(int posi, short value)
    {
        posi = arrayOffset + posi;
        array[posi] = short1(value);
        array[posi + 1] = short0(value);
    }
    
    @Override
    protected void _putLong(int posi, long value)
    {
        posi = arrayOffset + posi;
        array[posi] = long7(value);
        array[posi + 1] = long6(value);
        array[posi + 2] = long5(value);
        array[posi + 3] = long4(value);
        array[posi + 4] = long3(value);
        array[posi + 5] = long2(value);
        array[posi + 6] = long1(value);
        array[posi + 7] = long0(value);
    }
    
    @Override
    protected byte _get(int posi)
    {
        return array[arrayOffset + posi];
    }
    
    @Override
    public IoBuffer compact()
    {
        if (readPosi == 0)
        {
            return this;
        }
        int length = writePosi - readPosi;
        System.arraycopy(array, arrayOffset + readPosi, array, arrayOffset, length);
        readPosi = 0;
        writePosi = length;
        return this;
    }
    
    @Override
    public IoBuffer get(byte[] content, int off, int len)
    {
        checkBounds(content, off, len);
        if (remainRead() < len)
        {
            throw new IllegalArgumentException();
        }
        int r = nextReadPosi(len);
        System.arraycopy(array, arrayOffset + r, content, off, len);
        return this;
    }
}
