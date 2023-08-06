package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BufferRecycleTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("test");

    @Test
    public void test()
    {
        IoBuffer buffer = allocator.ioBuffer(12);
        buffer.free();
        IoBuffer buffer2 = allocator.ioBuffer(5689);
        assertTrue(buffer == buffer2);
        buffer2.free();
    }

    @Test
    public void test2() throws InterruptedException
    {
        final IoBuffer buffer = allocator.ioBuffer(12);
        Thread thread = new Thread(buffer::free);
        thread.start();
        thread.join();
        IoBuffer buffer2 = allocator.ioBuffer(2);
        assertTrue(buffer2 == buffer);
        buffer2.free();
    }

    @Test
    public void test3() throws InterruptedException
    {
        final IoBuffer       buffer = allocator.ioBuffer(128);
        final CountDownLatch latch  = new CountDownLatch(1);
        new FastThreadLocalThread(() ->
        {
            buffer.free();
            latch.countDown();
        }).start();
        latch.await();
        IoBuffer buffer2 = allocator.ioBuffer(128);
        assertEquals(System.identityHashCode(buffer),System.identityHashCode(buffer2));
        buffer2.free();
        IoBuffer buffer3 = allocator.ioBuffer(128);
        assertEquals(System.identityHashCode(buffer2),System.identityHashCode(buffer3));
    }
}
