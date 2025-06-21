package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ArenaAccepter;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import lombok.Getter;

public class PipelineStorageSegment extends StorageSegment implements ArenaAccepter
{
    protected     Arena         arena;
    protected     ChunkListNode chunkListNode;
    protected     long          handle;
    @Getter
    private final int           bitmapIndex;

    public PipelineStorageSegment(BufferAllocator allocator, int bitmapIndex)
    {
        super(allocator);
        this.bitmapIndex = bitmapIndex;
    }

    @Override
    public void init(Arena arena, ChunkListNode chunkListNode, long handle, int offset, int capacity)
    {
        this.arena         = arena;
        this.chunkListNode = chunkListNode;
        this.handle        = handle;
        init(chunkListNode.memory(), chunkListNode.directChunkAddress(), offset, capacity);
    }

    @Override
    protected void free0()
    {
        arena.free(chunkListNode, handle, capacity);
        arena         = null;
        chunkListNode = null;
        handle        = 0;
        super.free0();
    }

    public StorageSegment makeNewSegment(int newCapacity, BufferType bufferType)
    {
        PipelineStorageSegment newSegment = (PipelineStorageSegment) allocator.storageSegmentInstance();
        arena.allocate(newCapacity, newSegment);
        return newSegment;
    }
}
