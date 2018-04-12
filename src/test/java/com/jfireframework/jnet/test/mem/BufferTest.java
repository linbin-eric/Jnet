package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.buffer.PooledArchon;

public class BufferTest
{
    /**
     * heap扩容测试
     */
    @Test
    public void test()
    {
        Archon archon = PooledArchon.heapPooledArchon(4, 1);
        IoBuffer handler = IoBuffer.heapIoBuffer();
        archon.apply(2, handler);
        handler.put((byte) 0x01);
        assertEquals(1, handler.remainWrite());
        handler.put((byte) 0x01);
        handler.get();
        assertEquals(2, handler.capacity());
        assertEquals(0, handler.remainWrite());
        assertEquals(1, handler.getReadPosi());
        handler.put((byte) 0x01);
        assertEquals(4, handler.capacity());
        assertEquals(1, handler.remainWrite());
        assertEquals(1, handler.getReadPosi());
        handler.get();
        handler.put(new byte[2]);
        assertEquals(8, handler.capacity());
        assertEquals(3, handler.remainWrite());
        assertEquals(2, handler.getReadPosi());
    }
    
    /**
     * direct扩容测试
     */
    @Test
    public void test2()
    {
        Archon archon = PooledArchon.directPooledArchon(4, 1);
        IoBuffer handler = IoBuffer.directBuffer();
        archon.apply(2, handler);
        handler.put((byte) 0x01);
        assertEquals(1, handler.remainWrite());
        handler.put((byte) 0x01);
        handler.get();
        assertEquals(2, handler.capacity());
        assertEquals(0, handler.remainWrite());
        assertEquals(1, handler.getReadPosi());
        handler.put((byte) 0x01);
        assertEquals(4, handler.capacity());
        assertEquals(1, handler.remainWrite());
        assertEquals(1, handler.getReadPosi());
        handler.get();
        handler.put(new byte[2]);
        assertEquals(8, handler.capacity());
        assertEquals(3, handler.remainWrite());
        assertEquals(2, handler.getReadPosi());
    }
}
