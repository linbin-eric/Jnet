package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.buffer.Bits;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class UnPooledHeapBuffer extends UnPooledBuffer<byte[]>
{
    
    private ByteBuffer cachedNioBuffer;
    
    @Override
    public IoBuffer put(IoBuffer buffer, int len)
    {
        if (buffer.remainRead() < len)
        {
            throw new IllegalArgumentException("剩余读取长度不足");
        }
        if (buffer instanceof UnPooledHeapBuffer)
        {
            int posi = nextWritePosi(len);
            System.arraycopy(((UnPooledHeapBuffer) buffer).memory, buffer.getReadPosi(), memory, posi, len);
        }
        else if (buffer instanceof UnPooledDirectBuffer)
        {
            int posi = nextWritePosi(len);
            ByteBuffer byteBuffer = ((UnPooledDirectBuffer) buffer).memory;
            byteBuffer.position(buffer.getReadPosi());
            byteBuffer.get(memory, posi, len);
            byteBuffer.position(0);
        }
        else
        {
            super.put(buffer, len);
        }
        return this;
    }
    
    @Override
    public IoBuffer compact()
    {
        System.arraycopy(memory, readPosi, memory, 0, remainRead());
        writePosi -= readPosi;
        readPosi = 0;
        return this;
    }
    
    @Override
    public ByteBuffer byteBuffer()
    {
        if (cachedNioBuffer == null)
        {
            cachedNioBuffer = ByteBuffer.wrap(memory, readPosi, remainRead());
            return cachedNioBuffer;
        }
        else
        {
            cachedNioBuffer.clear();
            cachedNioBuffer.limit(remainRead());
            cachedNioBuffer.position(readPosi);
            return cachedNioBuffer;
        }
    }
    
    @Override
    public boolean isDirect()
    {
        return false;
    }
    
    @Override
    void put0(int posi, byte b)
    {
        memory[posi] = b;
    }
    
    @Override
    void put0(byte[] content, int off, int length, int writePosi)
    {
        System.arraycopy(content, off, memory, writePosi, length);
    }
    
    @Override
    void putInt0(int i, int posi)
    {
        Bits.putInt(memory, posi, i);
    }
    
    @Override
    void putShort0(short s, int posi)
    {
        Bits.putShort(memory, posi, s);
    }
    
    @Override
    void putLong0(long l, int posi)
    {
        Bits.putLong(memory, posi, l);
    }
    
    @Override
    void get0(byte[] content, int off, int length, int posi)
    {
        System.arraycopy(memory, posi, content, off, length);
    }
    
    @Override
    int getInt0(int posi)
    {
        return Bits.getInt(memory, posi);
    }
    
    @Override
    short getShort0(int posi)
    {
        return Bits.getShort(memory, posi);
    }
    
    @Override
    long getLong0(int posi)
    {
        return Bits.getLong(memory, posi);
    }
    
    @Override
    byte get0(int posi)
    {
        return memory[posi];
    }
    
    @Override
    void put1(UnPooledHeapBuffer buffer, int len)
    {
        int posi = nextWritePosi(len);
        System.arraycopy(buffer.memory, buffer.getReadPosi(), memory, posi, len);
    }
    
    @Override
    void put2(UnPooledDirectBuffer buffer, int len)
    {
        int posi = nextWritePosi(len);
        ByteBuffer byteBuffer = buffer.memory;
        byteBuffer.position(buffer.getReadPosi());
        byteBuffer.get(memory, posi, len);
        byteBuffer.position(0);
    }
}
