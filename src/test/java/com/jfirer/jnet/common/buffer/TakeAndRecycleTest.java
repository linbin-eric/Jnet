package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TakeAndRecycleTest
{
    int                   pagesize  = 4096;
    PooledBufferAllocator allocator = new PooledBufferAllocator(pagesize, 4, 1, true, "test");

    @Test
    public void test()
    {
        test0(true);
        test0(false);
    }

    private void test0(boolean preferDirect)
    {
        PoolableBuffer<?> buffer = (PoolableBuffer<?>) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(16, buffer.handle());
        PoolableBuffer<?> buffer2 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(9, buffer2.handle());
        ((AbstractBuffer) buffer).free();
        PoolableBuffer<?> buffer3 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(8, buffer3.handle());
        PoolableBuffer<?> buffer4 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(20, buffer4.handle());
        PoolableBuffer<?> buffer5 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(11, buffer5.handle());
        PoolableBuffer<?> buffer6 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize, preferDirect);
        assertEquals(21, buffer6.handle());
        ((AbstractBuffer) buffer2).free();
        PoolableBuffer<?> buffer7 = (PoolableBuffer<?>) allocator.ioBuffer(pagesize << 1, preferDirect);
        assertEquals(9, buffer7.handle());
    }
}
