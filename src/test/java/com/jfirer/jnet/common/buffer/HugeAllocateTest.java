package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HugeAllocateTest
{
    private static final long chunkSizeOffset     = UNSAFE.getFieldOffset("chunkSize", Arena.class);
    private static final long newChunkCountOffset = UNSAFE.getFieldOffset("newChunkCount", Arena.class);
    PooledBufferAllocator allocatorHeap   = new PooledBufferAllocator(100, false, new Arena("heap", BufferType.HEAP));
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator(100, true, new Arena("direct", BufferType.UNSAFE));

    @Test
    public void testHeap()
    {
        Arena         arena            = allocatorHeap.getArena();
        int          allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        PooledBuffer buffer           = (PooledBuffer) allocatorDirect.allocate(allocateCapacity);
        int          newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }

    private void test0(int allocateCapacity, PooledBuffer buffer, Arena arena)
    {
        assertTrue(buffer.getChunkListNode().isUnPooled());
        assertEquals(0, buffer.offset());
        assertEquals(allocateCapacity, buffer.capacity());
        buffer.free();
    }

    @Test
    public void testDirect()
    {
        Arena         arena            = allocatorDirect.getArena();
        int           allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        int          newChunkCount = UNSAFE.getInt(arena, newChunkCountOffset);
        PooledBuffer buffer        = (PooledBuffer) allocatorDirect.allocate(allocateCapacity);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }
}
