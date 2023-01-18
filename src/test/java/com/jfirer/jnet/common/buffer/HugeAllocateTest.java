package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.arena.impl.AbstractArena;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PoolableBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class HugeAllocateTest
{
    PooledBufferAllocator allocator = new PooledBufferAllocator("HugeTest");
    private static final long chunkSizeOffset     = UNSAFE.getFieldOffset("chunkSize", AbstractArena.class);
    private static final long newChunkCountOffset = UNSAFE.getFieldOffset("newChunkCount", AbstractArena.class);

    @Test
    public void testHeap()
    {
        AbstractArena<?>  arena            = (AbstractArena<?>) allocator.currentArena(false);
        int               allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        PoolableBuffer<?> buffer           = (PoolableBuffer<?>) allocator.heapBuffer(allocateCapacity);
        int               newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }

    private void test0(int allocateCapacity, PoolableBuffer<?> buffer, AbstractArena<?> arena)
    {
        assertTrue(buffer.chunk().isUnPooled());
        assertEquals(0, ((AbstractBuffer) buffer).offset());
        assertEquals(allocateCapacity, ((AbstractBuffer) buffer).capacity());
        ((AbstractBuffer) buffer).free();
    }

    @Test
    public void testDirect()
    {
        AbstractArena<?>  arena            = (AbstractArena<?>) allocator.currentArena(true);
        int               allocateCapacity = UNSAFE.getInt(arena, chunkSizeOffset) + 1;
        int               newChunkCount    = UNSAFE.getInt(arena, newChunkCountOffset);
        PoolableBuffer<?> buffer           = (PoolableBuffer<?>) allocator.unsafeBuffer(allocateCapacity);
        test0(allocateCapacity, buffer, arena);
        assertEquals(UNSAFE.getInt(arena, newChunkCountOffset), newChunkCount);
    }
}
