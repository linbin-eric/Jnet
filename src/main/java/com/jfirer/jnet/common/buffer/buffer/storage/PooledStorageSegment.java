package com.jfirer.jnet.common.buffer.buffer.storage;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Getter;
import lombok.Setter;

@Getter
public class PooledStorageSegment extends StorageSegment
{
    protected Arena          arena;
    protected ChunkListNode  chunkListNode;
    protected long           handle;
    @Setter
    protected RecycleHandler recycleHandler;

    public PooledStorageSegment(BufferAllocator allocator)
    {
        super(allocator);
    }

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
        PooledStorageSegment newSegment = (PooledStorageSegment) allocator.storageSegmentInstance();
        arena.allocate(newCapacity, newSegment);
        return newSegment;
    }
}
