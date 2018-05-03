package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

public class TakeAndRecycleTest
{
    @Test
    public void test()
    {
        Chunk chunk = Chunk.newHeapChunk(4, 1);
        PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
        assertTrue(chunk.apply(1, buffer, false));
        assertEquals(16, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(9, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(10, buffer.index());
        assertTrue(chunk.apply(4, buffer, false));
        assertEquals(6, buffer.index());
        assertFalse(chunk.apply(8, buffer, false));
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(11, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(14, buffer.index());
        assertTrue(chunk.apply(1, buffer, false));
        assertEquals(17, buffer.index());
        
    }
    
    @Test
    public void test2()
    {
        Chunk chunk = Chunk.newDirectChunk(4, 1);
        PooledIoBuffer buffer = PooledIoBuffer.directBuffer();
        assertTrue(chunk.apply(1, buffer, false));
        assertEquals(16, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(9, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(10, buffer.index());
        assertTrue(chunk.apply(4, buffer, false));
        assertEquals(6, buffer.index());
        assertFalse(chunk.apply(8, buffer, false));
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(11, buffer.index());
        assertTrue(chunk.apply(2, buffer, false));
        assertEquals(14, buffer.index());
        assertTrue(chunk.apply(1, buffer, false));
        assertEquals(17, buffer.index());
        
    }
    
    /**
     * 使用一维数组来表达平衡二叉树
     */
    @Test
    public void test3()
    {
        Chunk pooledMem = Chunk.newHeapChunk(4, 128);
        PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
        pooledMem.apply(400, buffer, false);
        assertEquals(4, buffer.index());
        PooledIoBuffer buffer2 = PooledIoBuffer.heapBuffer();
        pooledMem.apply(100, buffer2, false);
        assertEquals(20, buffer2.index());
        PooledIoBuffer buffer3 = PooledIoBuffer.heapBuffer();
        pooledMem.apply(200, buffer3, false);
        assertEquals(11, buffer3.index());
        PooledIoBuffer buffer4 = PooledIoBuffer.heapBuffer();
        pooledMem.apply(100, buffer4, false);
        assertEquals(21, buffer4.index());
        PooledIoBuffer buffer5 = PooledIoBuffer.heapBuffer();
        pooledMem.apply(500, buffer5, false);
        assertEquals(6, buffer5.index());
        pooledMem.recycle(buffer3.index());
        pooledMem.recycle(buffer4.index());
        pooledMem.recycle(buffer2.index());
        PooledIoBuffer buffer6 = PooledIoBuffer.heapBuffer();
        pooledMem.apply(400, buffer6, false);
        assertEquals(5, buffer6.index());
    }
}
