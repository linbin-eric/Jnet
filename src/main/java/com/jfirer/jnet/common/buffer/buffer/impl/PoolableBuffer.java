package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.impl.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public abstract class PoolableBuffer<T> extends AbstractBuffer<T>
{
    protected Arena<T>         arena;
    protected ChunkListNode<T> chunkListNode;
    protected long             handle;

    public void init(Arena<T> arena, ChunkListNode<T> chunkListNode, int capacity, int offset, long handle)
    {
        this.arena = arena;
        this.chunkListNode = chunkListNode;
        this.handle = handle;
        init(chunkListNode.memory(), capacity, offset);
    }

    public Chunk<T> chunk()
    {
        return chunkListNode;
    }

    public long handle()
    {
        return handle;
    }

    @Override
    protected long getNativeAddress(T memory)
    {
        if (arena != null)
        {
            return chunkListNode.directChunkAddress();
        }
        else if (bufferType() == BufferType.DIRECT || bufferType() == BufferType.UNSAFE)
        {
            return PlatFormFunction.bytebufferOffsetAddress((ByteBuffer) memory);
        }
        //因为Memory还处于孵化之中，性能上也是最差的，因此先放弃。
//        else if (memory instanceof ByteBuffer buffer)
//        {
//            return PlatFormFunction.bytebufferOffsetAddress(buffer);
//        }
//        else if (memory instanceof MemorySegment segment)
//        {
//            return segment.address().toRawLongValue();
//        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    @Override
    protected void reAllocate(int reqCapacity)
    {
        arena.reAllocate(chunkListNode, this, reqCapacity);
    }

    @Override
    protected void free0(int capacity)
    {
        arena.free(chunkListNode, handle, capacity);
    }
}
