package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class SliceBufferTest
{
    private boolean         preDirect;
    private BufferAllocator allocator;

    public SliceBufferTest(boolean preDirect, BufferAllocator allocator)
    {
        this.preDirect = preDirect;
        this.allocator = allocator;
    }

    @Parameterized.Parameters
    public static Collection<?> data()
    {
        return Arrays.asList(new Object[][]{ //
                {true, PooledBufferAllocator.DEFAULT},//
                {false, PooledBufferAllocator.DEFAULT},//
//                {true, UnPooledRecycledBufferAllocator.DEFAULT},//
//                {false, UnPooledUnRecycledBufferAllocator.DEFAULT},//
        });
    }

    @Test
    public void test()
    {
        IoBuffer buffer = allocator.ioBuffer(128, preDirect);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(3);
        IoBuffer slice = buffer.slice(4);
        assertEquals(4, buffer.getReadPosi());
        assertEquals(2, ((AbstractBuffer) buffer).refCount());
        assertEquals(4, slice.getWritePosi());
        assertEquals(4, slice.capacity());
        assertEquals(0, slice.getReadPosi());
        assertEquals(1, slice.getInt());
        slice.setReadPosi(0);
        IoBuffer slice2 = slice.slice(4);
        assertEquals(2, ((AbstractBuffer) buffer).refCount());
        assertEquals(2, ((AbstractBuffer) slice).refCount());
        assertEquals(1, ((AbstractBuffer) slice2).refCount());
        assertEquals(4, slice.getReadPosi());
        assertEquals(4, slice2.getWritePosi());
        assertEquals(0, slice2.getReadPosi());
        assertEquals(1, slice2.getInt());
        slice.free();
        assertEquals(1, ((AbstractBuffer) slice).refCount());
        assertEquals(2, ((AbstractBuffer) buffer).refCount());
        assertEquals(1, ((AbstractBuffer) slice2).refCount());
        assertNotNull(((AbstractBuffer) slice).memory());
        slice2.free();
        assertEquals(0, ((AbstractBuffer) slice).refCount());
        assertNull(((AbstractBuffer) slice).memory());
        assertNotNull(((AbstractBuffer) buffer).memory());
        buffer.free();
        assertEquals(0, ((AbstractBuffer) buffer).refCount());
    }

    @Test
    public void test1()
    {
        IoBuffer buffer = allocator.ioBuffer(128, preDirect);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(3);
        IoBuffer slice  = buffer.slice(4);
        IoBuffer slice2 = slice.slice(4);
        buffer.free();
        slice.free();
        assertNotNull(((AbstractBuffer) buffer).memory());
        assertNotNull(((AbstractBuffer) slice).memory());
        slice2.free();
        assertNull(((AbstractBuffer) buffer).memory());
        assertNull(((AbstractBuffer) slice).memory());
        assertNull(((AbstractBuffer) slice2).memory());
    }

    @Test
    public void test2()
    {
        IoBuffer buffer = allocator.ioBuffer(128, preDirect);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(3);
        IoBuffer slice = buffer.slice(4);
        slice.free();
        buffer.free();
        try
        {
            buffer.free();
        }
        catch (Throwable e)
        {
            assertTrue(e instanceof IllegalStateException);
        }
    }
}
