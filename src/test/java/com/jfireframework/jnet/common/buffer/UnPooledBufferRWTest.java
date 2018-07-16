package com.jfireframework.jnet.common.buffer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class UnPooledBufferRWTest
{
    private IoBuffer buffer;
    private IoBuffer paramBuffer;
    
    @Parameters
    public static Collection<?> data()
    {
        return Arrays.asList(new Object[][] { //
                { UnPooledUnRecycledBufferAllocator.DEFAULT.heapBuffer(128), UnPooledUnRecycledBufferAllocator.DEFAULT.heapBuffer(30) }, //
                { UnPooledUnRecycledBufferAllocator.DEFAULT.heapBuffer(128), UnPooledUnRecycledBufferAllocator.DEFAULT.directBuffer(30) }, //
                { UnPooledUnRecycledBufferAllocator.DEFAULT.directBuffer(128), UnPooledUnRecycledBufferAllocator.DEFAULT.heapBuffer(30) }, //
                { UnPooledUnRecycledBufferAllocator.DEFAULT.directBuffer(128), UnPooledUnRecycledBufferAllocator.DEFAULT.directBuffer(30) }, //
                { UnPooledRecycledBufferAllocator.DEFAULT.heapBuffer(128), UnPooledRecycledBufferAllocator.DEFAULT.heapBuffer(30) }, //
                { UnPooledRecycledBufferAllocator.DEFAULT.heapBuffer(128), UnPooledRecycledBufferAllocator.DEFAULT.directBuffer(30) }, //
                { UnPooledRecycledBufferAllocator.DEFAULT.directBuffer(128), UnPooledRecycledBufferAllocator.DEFAULT.heapBuffer(30) }, //
                { UnPooledRecycledBufferAllocator.DEFAULT.directBuffer(128), UnPooledRecycledBufferAllocator.DEFAULT.directBuffer(30) },//
        });
    }
    
    public UnPooledBufferRWTest(IoBuffer buffer, IoBuffer paramBuffer)
    {
        this.buffer = buffer;
        this.paramBuffer = paramBuffer;
    }
    
    @Before
    public void before()
    {
        buffer.clearAndErasureData();
        paramBuffer.clearAndErasureData();
    }
    
    @Test
    public void test()
    {
        assertEquals(128, buffer.capacity());
        buffer.put((byte) 27);
        buffer.addWritePosi(1);
        buffer.put(new byte[] { 36, 90 });
        buffer.addWritePosi(1);
        buffer.putInt(5);
        buffer.putLong(12564l);
        buffer.putShort((short) 1000);
        assertEquals((byte) 27, buffer.get());
        buffer.addReadPosi(1);
        assertEquals((byte) 36, buffer.get());
        assertEquals((byte) 90, buffer.get());
        buffer.addReadPosi(1);
        assertEquals(5, buffer.getInt());
        assertEquals(12564l, buffer.getLong());
        assertEquals((short) 1000, buffer.getShort());
        assertNotEquals(100, buffer.getReadPosi());
        assertNotEquals(101, buffer.getWritePosi());
        buffer.setWritePosi(101);
        buffer.setReadPosi(100);
        assertEquals(27, buffer.remainWrite());
        assertEquals(1, buffer.remainRead());
        assertEquals(100, buffer.getReadPosi());
        assertEquals(101, buffer.getWritePosi());
        assertEquals((byte) 0, buffer.get(100));
        buffer.put((byte) 12, 100);
        assertEquals((byte) 12, buffer.get(100));
        buffer.clear();
        byte[] content = new byte[] { 12, 20, 35 };
        buffer.put(content);
        assertEquals(12, buffer.get());
        assertEquals(20, buffer.get());
        assertEquals(35, buffer.get());
        assertEquals(3, buffer.getReadPosi());
        assertEquals(0, buffer.remainRead());
        buffer.clear();
        buffer.putInt(3);
        buffer.putShort((short) 2);
        buffer.putLong(213121l);
        assertEquals(3, buffer.getInt());
        assertEquals(2, buffer.getShort());
        assertEquals(213121l, buffer.getLong());
        buffer.clear();
        buffer.putInt(3, 0);
        buffer.putShort((short) 2, 6);
        buffer.putLong(1000l, 12);
        buffer.setWritePosi(20);
        assertEquals(3, buffer.setReadPosi(0).getInt());
        assertEquals((short) 2, buffer.setReadPosi(6).getShort());
        assertEquals(1000l, buffer.setReadPosi(12).getLong());
        buffer.clear();
        buffer.put((byte) 12);
        buffer.put((byte) 13);
        buffer.put((byte) 14);
        byte[] result = new byte[3];
        buffer.get(result);
        assertArrayEquals(new byte[] { 12, 13, 14 }, result);
        buffer.clear();
        buffer.put((byte) 13);
        buffer.put((byte) 13);
        buffer.put((byte) 17);
        buffer.put((byte) 27);
        buffer.put((byte) 10);
        buffer.put((byte) 12);
        buffer.put((byte) 17);
        buffer.put((byte) 20);
        byte[] indexs = new byte[] { 17, 20 };
        assertEquals(6, buffer.indexOf(indexs));
        buffer.addWritePosi(-1);
        assertEquals(-1, buffer.indexOf(indexs));
        buffer.addWritePosi(-1);
        assertEquals(-1, buffer.indexOf(indexs));
        buffer.clear();
        buffer.putInt(4, 45);
        buffer.putShort((short) 2, 90);
        buffer.putLong(100, 100);
        buffer.setWritePosi(124);
        assertEquals(4, buffer.getInt(45));
        assertEquals(2, buffer.getShort(90));
        assertEquals(100, buffer.getLong(100));
    }
    
    // 测试放入buffer
    @Test
    public void test2()
    {
        buffer.addReadPosi(2).addWritePosi(2);
        paramBuffer.putInt(3);
        paramBuffer.putShort((short) 100);
        paramBuffer.putLong(1000l);
        buffer.put(paramBuffer);
        assertEquals(16, buffer.getWritePosi());
        assertEquals(3, buffer.getInt());
        assertEquals((short) 100, buffer.getShort());
        assertEquals(1000l, buffer.getLong());
        buffer.free();
    }
    
    /**
     * 检查异常情况是否捕获
     */
    @Test
    public void test3()
    {
        buffer.addWritePosi(125);
        try
        {
            buffer.putInt(4);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        buffer.addReadPosi(125);
        try
        {
            buffer.getInt();
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        buffer.clear();
        try
        {
            buffer.putInt(10, 125);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        try
        {
            buffer.clear().getInt(123);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        buffer.clear();
        paramBuffer.clear().setWritePosi(10).setReadPosi(4);
        try
        {
            buffer.put(paramBuffer, 20);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
    }
    
    /**
     * 测试压缩功能
     */
    @Test
    public void test4()
    {
        buffer.addWritePosi(10);
        buffer.putInt(3);
        for (int i = 0; i < 10; i++)
        {
            assertEquals((byte) 0, buffer.get());
        }
        assertEquals(14, buffer.getWritePosi());
        buffer.compact();
        assertEquals(0, buffer.getReadPosi());
        assertEquals(4, buffer.getWritePosi());
        assertEquals(3, buffer.getInt());
    }
    
    /**
     * 测试ByteBuffer接口
     */
    @Test
    public void test5()
    {
        buffer.putInt(4);
        buffer.putShort((short) 2);
        buffer.putLong(23l);
        ByteBuffer nioBuffer = buffer.readableByteBuffer();
        assertEquals(4, nioBuffer.getInt());
        assertEquals(2, nioBuffer.getShort());
        assertEquals(23l, nioBuffer.getLong());
        nioBuffer = buffer.readableByteBuffer();
        assertEquals(4, nioBuffer.getInt());
        assertEquals(2, nioBuffer.getShort());
        assertEquals(23l, nioBuffer.getLong());
    }
}
