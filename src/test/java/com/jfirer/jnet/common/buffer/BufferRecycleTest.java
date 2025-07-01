package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator2;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class BufferRecycleTest
{
    PooledBufferAllocator2 allocator = new PooledBufferAllocator2(100, true, new Arena("default", BufferType.UNSAFE));

    @Test
    public void test()
    {
        IoBuffer buffer = allocator.ioBuffer(12);
        buffer.free();
        IoBuffer buffer2 = allocator.ioBuffer(5689);
        assertSame(buffer, buffer2);
        buffer2.free();
    }

    @Test
    public void test2() throws InterruptedException
    {
        final IoBuffer buffer = allocator.ioBuffer(12);
        Thread         thread = new Thread(buffer::free);
        thread.start();
        thread.join();
        IoBuffer buffer2 = allocator.ioBuffer(12);
        assertTrue(buffer == buffer2);
        buffer2.free();
    }

    @Test
    public void test3() throws InterruptedException
    {
        final IoBuffer       buffer = allocator.ioBuffer(128);
        final CountDownLatch latch  = new CountDownLatch(1);
        new FastThreadLocalThread(() -> {
            buffer.free();
            latch.countDown();
        }).start();
        latch.await();
        IoBuffer buffer2 = allocator.ioBuffer(128);
        assertEquals(System.identityHashCode(buffer), System.identityHashCode(buffer2));
        buffer2.free();
        IoBuffer buffer3 = allocator.ioBuffer(128);
        assertEquals(System.identityHashCode(buffer2), System.identityHashCode(buffer3));
    }
}
