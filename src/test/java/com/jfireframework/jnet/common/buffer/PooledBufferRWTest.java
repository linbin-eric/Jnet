package com.jfireframework.jnet.common.buffer;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class PooledBufferRWTest
{
    private IoBuffer buffer;
    private IoBuffer paramBuffer;

    public PooledBufferRWTest(IoBuffer buffer, IoBuffer paramBuffer)
    {
        this.buffer = buffer;
        this.paramBuffer = paramBuffer;
    }

    @Parameters
    public static Collection<?> data()
    {
        return Arrays.asList(new Object[][]{ //
                {allocate(128), allocate(30)}, //
                {allocate(128), allocateDirect(30)}, //
                {allocateDirect(128), allocate(30)}, //
                {allocateDirect(128), allocateDirect(30)},//
        });
    }

    static PooledBuffer<?> allocate(int size)
    {
        return (PooledBuffer<?>) PooledBufferAllocator.DEFAULT.heapBuffer(size);
    }

    static PooledBuffer<?> allocateDirect(int size)
    {
        return (PooledBuffer<?>) PooledBufferAllocator.DEFAULT.directBuffer(size);
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
        buffer.put(new byte[]{36, 90});
        buffer.addWritePosi(1);
        buffer.putInt(5);
        buffer.putLong(12564L);
        buffer.putShort((short) 1000);
        assertEquals((byte) 27, buffer.get());
        buffer.addReadPosi(1);
        assertEquals((byte) 36, buffer.get());
        assertEquals((byte) 90, buffer.get());
        buffer.addReadPosi(1);
        assertEquals(5, buffer.getInt());
        assertEquals(12564L, buffer.getLong());
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
        byte[] content = new byte[]{12, 20, 35};
        buffer.put(content);
        assertEquals(12, buffer.get());
        assertEquals(20, buffer.get());
        assertEquals(35, buffer.get());
        assertEquals(3, buffer.getReadPosi());
        assertEquals(0, buffer.remainRead());
        buffer.clear();
        buffer.putInt(3);
        buffer.putShort((short) 2);
        buffer.putLong(213121L);
        assertEquals(3, buffer.getInt());
        assertEquals(2, buffer.getShort());
        assertEquals(213121L, buffer.getLong());
        buffer.clear();
        buffer.putInt(3, 0);
        buffer.putShort((short) 2, 6);
        buffer.putLong(1000L, 12);
        buffer.setWritePosi(20);
        assertEquals(3, buffer.setReadPosi(0).getInt());
        assertEquals((short) 2, buffer.setReadPosi(6).getShort());
        assertEquals(1000L, buffer.setReadPosi(12).getLong());
        buffer.clear();
        buffer.put((byte) 12);
        buffer.put((byte) 13);
        buffer.put((byte) 14);
        byte[] result = new byte[3];
        buffer.get(result);
        assertArrayEquals(new byte[]{12, 13, 14}, result);
        buffer.clear();
        buffer.put((byte) 13);
        buffer.put((byte) 13);
        buffer.put((byte) 17);
        buffer.put((byte) 27);
        buffer.put((byte) 10);
        buffer.put((byte) 12);
        buffer.put((byte) 17);
        buffer.put((byte) 20);
        byte[] indexs = new byte[]{17, 20};
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
        buffer.addWritePosi(4).addReadPosi(4);
        paramBuffer.putInt(3);
        paramBuffer.putShort((short) 100);
        paramBuffer.putLong(1000L);
        buffer.put(paramBuffer);
        assertEquals(18, buffer.getWritePosi());
        assertEquals(3, buffer.getInt());
        assertEquals((short) 100, buffer.getShort());
        assertEquals(1000L, buffer.getLong());
    }

    /**
     * 检查异常情况覆盖
     */
    @Test
    public void test3()
    {
        buffer.addReadPosi(125);
        try
        {
            buffer.getInt();
        } catch (Exception e)
        {
            assertTrue(e instanceof IllegalArgumentException);
        }
        buffer.clear();
        try
        {
            buffer.clear().getInt(123);
        } catch (Exception e)
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
        buffer.putLong(23L);
        ByteBuffer nioBuffer = buffer.readableByteBuffer();
        assertEquals(4, nioBuffer.getInt());
        assertEquals(2, nioBuffer.getShort());
        assertEquals(23L, nioBuffer.getLong());
        nioBuffer = buffer.readableByteBuffer();
        assertEquals(4, nioBuffer.getInt());
        assertEquals(2, nioBuffer.getShort());
        assertEquals(23L, nioBuffer.getLong());
    }
}
