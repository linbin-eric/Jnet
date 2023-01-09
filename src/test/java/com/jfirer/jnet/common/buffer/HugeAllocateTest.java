package com.jfirer.jnet.common.buffer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HugeAllocateTest
{
    PooledBufferAllocator allocator = PooledBufferAllocator.DEFAULT;

    @Test
    public void testHeap()
    {
        ThreadCache      threadCache      = allocator.threadCache();
        AbstractArena<?> arena            = threadCache.heapArena;
        int              allocateCapacity = arena.chunkSize + 1;
        PooledBuffer<?>  buffer           = (PooledBuffer<?>) allocator.heapBuffer(allocateCapacity);
        int              newChunkCount    = arena.newChunkCount;
        test0(allocateCapacity, buffer, arena);
        assertEquals(arena.newChunkCount, newChunkCount);
    }

    private void test0(int allocateCapacity, PooledBuffer<?> buffer, AbstractArena<?> arena)
    {
        assertTrue(buffer.chunk().unpooled);
        assertEquals(0, ((AbstractBuffer) buffer).offset);
        assertEquals(allocateCapacity, ((AbstractBuffer) buffer).capacity);
        ((AbstractBuffer) buffer).free();
    }

    @Test
    public void testDirect()
    {
        ThreadCache      threadCache      = allocator.threadCache();
        AbstractArena<?> arena            = threadCache.directArena;
        int              allocateCapacity = arena.chunkSize + 1;
        int              newChunkCount    = arena.newChunkCount;
        PooledBuffer<?>  buffer           = (PooledBuffer<?>) allocator.directBuffer(allocateCapacity);
        test0(allocateCapacity, buffer, arena);
        assertEquals(arena.newChunkCount, newChunkCount);
    }
}
