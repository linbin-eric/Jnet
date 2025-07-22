package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ArenaAccepter;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;

@ToString
public class PooledBuffer extends UnPooledBuffer implements ArenaAccepter
{
    protected       Arena         arena;
    @Getter
    protected       ChunkListNode chunkListNode;
    @Getter
    protected       long          handle;
    /**
     * -1代表这个对象本身不是池化的。
     */
    @Getter
    protected final int           bitmapIndex;

    public PooledBuffer(BufferType bufferType, BufferAllocator allocator, int bitmapIndex)
    {
        super(bufferType, allocator);
        this.bitmapIndex = bitmapIndex;
    }

    @Override
    public void init(Arena arena, ChunkListNode chunkListNode, long handle, int offset, int capacity)
    {
        this.arena         = arena;
        this.chunkListNode = chunkListNode;
        this.handle        = handle;
        init(chunkListNode.memory(), chunkListNode.directChunkAddress(), capacity, offset, 0, capacity);
    }

    @Override
    protected void freeMemory0()
    {
        arena.free(chunkListNode, handle, memoryCapacity);
        arena         = null;
        chunkListNode = null;
        handle        = 0;
    }

    @Override
    protected void expansionCapacity(int newCapacity)
    {
        Arena         oldArena          = arena;
        ChunkListNode oldChunkListNode  = chunkListNode;
        long          oldHandle         = handle;
        int           oldMemoryCapacity = memoryCapacity;
        AtomicInteger oldRefCnt         = refCnt;
        super.expansionCapacity(newCapacity);
        //如果没有变化，意味着当前refCnt是最后一次持有旧Arena的对象，应该对旧的chunk进行回收
        if (oldRefCnt == refCnt)
        {
            oldArena.free(oldChunkListNode, oldHandle, oldMemoryCapacity);
        }
    }

    @Override
    public IoBuffer slice(int length)
    {
        PooledBuffer slice = (PooledBuffer) super.slice(length);
        slice.arena = arena;
        slice.chunkListNode =chunkListNode;
        slice.handle = handle;
        return slice;

    }

    @Override
    protected void compactByNewSpace(int length)
    {
        Arena         oldArena         = arena;
        ChunkListNode oldChunkListNode = chunkListNode;
        long          oldHandle        = handle;
        int           oldMemoryCapacity = memoryCapacity;
        AtomicInteger oldRefCnt         = refCnt;
        super.compactByNewSpace(length);
        if (oldRefCnt == refCnt)
        {
            oldArena.free(oldChunkListNode,oldHandle,oldMemoryCapacity);
        }
    }

    @Override
    public String toString()
    {
        return "PooledBuffer{" + ", bitmapIndex=" + bitmapIndex + ", refCnt=" + refCnt+ ",capacity=" + bufferCapacity + ", readPosi=" + readPosi + ", writePosi=" + writePosi + ", refCount=" + refCnt.get() + '}';
    }
}
