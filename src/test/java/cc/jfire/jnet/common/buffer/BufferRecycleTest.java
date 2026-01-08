package cc.jfire.jnet.common.buffer;

import cc.jfire.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import cc.jfire.jnet.common.buffer.arena.Arena;
import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class BufferRecycleTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator(100, true, new Arena("default", BufferType.UNSAFE));

    @Test
    public void test()
    {
        IoBuffer buffer = allocator.allocate(12);
        buffer.free();
        IoBuffer buffer2 = allocator.allocate(5689);
        assertSame(buffer, buffer2);
        buffer2.free();
    }

    @Test
    public void test2() throws InterruptedException
    {
        final IoBuffer buffer = allocator.allocate(12);
        Thread         thread = new Thread(buffer::free);
        thread.start();
        thread.join();
        IoBuffer buffer2 = allocator.allocate(12);
        assertTrue(buffer == buffer2);
        buffer2.free();
    }
}
