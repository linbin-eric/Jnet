package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.BasicBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.StorageSegment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class SliceBufferTest
{
    private final BufferAllocator allocator;

    public SliceBufferTest(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    @Parameterized.Parameters
    public static Collection<?> data()
    {
        return Arrays.asList(new Object[][]{ //
                {new PooledBufferAllocator("testDirect", true)},//
                {new PooledBufferAllocator("testHeap", false)},//
        });
    }

    @Test
    public void test()
    {
        IoBuffer buffer = allocator.ioBuffer(128);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(3);
        IoBuffer slice = buffer.slice(4);
        assertEquals(4, buffer.getReadPosi());
        assertEquals(2, buffer.refCount());
        assertEquals(4, slice.getWritePosi());
        assertEquals(4, slice.capacity());
        assertEquals(0, slice.getReadPosi());
        assertEquals(1, slice.getInt());
        slice.setReadPosi(0);
        IoBuffer slice2 = slice.slice(4);
        assertEquals(3, buffer.refCount());
        assertEquals(3, slice.refCount());
        assertEquals(3, slice2.refCount());
        assertEquals(4, slice.getReadPosi());
        assertEquals(4, slice2.getWritePosi());
        assertEquals(0, slice2.getReadPosi());
        assertEquals(1, slice2.getInt());
        slice.free();
        assertEquals(2, buffer.refCount());
        assertEquals(2, slice2.refCount());
        slice2.free();
        assertEquals(1, buffer.refCount());
        StorageSegment storageSegment = ((BasicBuffer) buffer).getStorageSegment();
        buffer.free();
        assertEquals(0, storageSegment.getRefCount());
    }

    @Test
    public void test1()
    {
        IoBuffer buffer = allocator.ioBuffer(128);
        buffer.putInt(1);
        buffer.putInt(2);
        buffer.putInt(3);
        BasicBuffer slice  = (BasicBuffer) buffer.slice(4);
        BasicBuffer slice2 = (BasicBuffer) slice.slice(4);
        buffer.free();
        slice.free();
        assertEquals(1, slice2.refCount());
        slice2.free();
    }

    @Test
    public void test2()
    {
        IoBuffer buffer = allocator.ioBuffer(128);
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
            assertTrue(e instanceof NullPointerException);
        }
    }
}
