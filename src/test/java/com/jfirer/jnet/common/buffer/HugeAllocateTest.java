package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HugeAllocateTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("HugeTest");
    private static final long chunkSizeOffset     = UNSAFE.getFieldOffset("chunkSize", Arena.class);
    private static final long newChunkCountOffset = UNSAFE.getFieldOffset("newChunkCount", Arena.class);

    @Test
    public void testHeap()
    {
        Arena        arena            = (Arena) allocator.currentArena(false);
        int          allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        PooledBuffer buffer           = (PooledBuffer) allocator.heapBuffer(allocateCapacity);
        int          newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }

    private void test0(int allocateCapacity, PooledBuffer buffer, Arena arena)
    {
        assertTrue(buffer.chunk().isUnPooled());
        assertEquals(0, ((AbstractBuffer) buffer).offset());
        assertEquals(allocateCapacity, ((AbstractBuffer) buffer).capacity());
        ((AbstractBuffer) buffer).free();
    }

    @Test
    public void testDirect()
    {
        Arena        arena            = (Arena) allocator.currentArena(true);
        int          allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        int          newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        PooledBuffer buffer           = (PooledBuffer) allocator.unsafeBuffer(allocateCapacity);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }
}
