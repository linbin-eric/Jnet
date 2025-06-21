package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HugeAllocateTest
{
    private static final long chunkSizeOffset     = UNSAFE.getFieldOffset("chunkSize", Arena.class);
    private static final long newChunkCountOffset = UNSAFE.getFieldOffset("newChunkCount", Arena.class);
    PooledBufferAllocator allocatorHeap = new PooledBufferAllocator("HugeTestHeap");
    PooledBufferAllocator allocatorDirect = new PooledBufferAllocator("HugeTestDirect");

    @Test
    public void testHeap()
    {
        Arena       arena            = allocatorHeap.currentArena();
        int            allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        UnPooledBuffer buffer           = (UnPooledBuffer) allocatorDirect.ioBuffer(allocateCapacity);
        int            newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }

    private void test0(int allocateCapacity, UnPooledBuffer buffer, Arena arena)
    {
        PooledStorageSegment storageSegment = (PooledStorageSegment) buffer.getStorageSegment();
        assertTrue(storageSegment.getChunkListNode().isUnPooled());
        assertEquals(0, buffer.offset());
        assertEquals(allocateCapacity, buffer.capacity());
        buffer.free();
    }

    @Test
    public void testDirect()
    {
        Arena       arena            = allocatorDirect.currentArena();
        int         allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        int            newChunkCount = UNSAFE.getInt(arena, newChunkCountOffset);
        UnPooledBuffer buffer        = (UnPooledBuffer) allocatorDirect.ioBuffer(allocateCapacity);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }
}
