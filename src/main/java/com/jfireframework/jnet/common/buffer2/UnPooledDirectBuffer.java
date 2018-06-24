package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class UnPooledDirectBuffer extends UnPooledBuffer<ByteBuffer>
{
    
    @Override
    public IoBuffer compact()
    {
        memory.limit(writePosi).position(readPosi);
        memory.compact();
        writePosi -= readPosi;
        readPosi = 0;
        memory.limit(capacity).position(0);
        return this;
    }
    
    @Override
    public ByteBuffer byteBuffer()
    {
        memory.limit(writePosi).position(readPosi);
        return memory;
    }
    
    @Override
    public boolean isDirect()
    {
        return true;
    }
    
    @Override
    void get0(byte[] content, int off, int length, int posi)
    {
        memory.position(posi);
        memory.get(content, off, length);
        memory.position(0);
    }
    
    @Override
    byte get0(int posi)
    {
        return memory.get(posi);
    }
    
    @Override
    void put0(int posi, byte b)
    {
        memory.put(posi, b);
    }
    
    @Override
    void put0(byte[] content, int off, int length, int writePosi)
    {
        memory.position(writePosi);
        memory.put(content, off, length);
        memory.position(0);
    }
    
    @Override
    void putInt0(int i, int posi)
    {
        memory.putInt(posi, i);
    }
    
    @Override
    void putShort0(short s, int posi)
    {
        memory.putShort(posi, s);
    }
    
    @Override
    void putLong0(long l, int posi)
    {
        memory.putLong(posi, l);
    }
    
    @Override
    int getInt0(int posi)
    {
        return memory.getInt(posi);
    }
    
    @Override
    short getShort0(int posi)
    {
        return memory.getShort(posi);
    }
    
    @Override
    long getLong0(int posi)
    {
        return memory.getLong(posi);
    }
    
    @Override
    void put1(UnPooledHeapBuffer buffer, int len)
    {
        int posi = nextWritePosi(len);
        memory.position(posi);
        byte[] content = buffer.memory;
        memory.put(content, buffer.getReadPosi(), len);
        memory.position(0);
    }
    
    @Override
    void put2(UnPooledDirectBuffer buffer, int len)
    {
        int posi = nextWritePosi(len);
        memory.position(posi);
        ByteBuffer param = buffer.memory;
        param.position(readPosi).limit(readPosi + len);
        memory.put(param);
        param.position(0).limit(buffer.capacity());
        memory.position(0);
    }
    
}
