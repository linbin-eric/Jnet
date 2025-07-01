package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.ArenaAccepter;
import com.jfirer.jnet.common.buffer.arena.ChunkListNode;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Getter;

import java.awt.image.AreaAveragingScaleFilter;

public class PooledBuffer2 extends UnPooledBuffer2 implements ArenaAccepter
{
    protected       Arena         arena;
    protected       ChunkListNode chunkListNode;
    protected       long          handle;
    /**
     * -1代表这个对象本身不是池化的。
     */
    @Getter
    protected final int           bitmapIndex;

    public PooledBuffer2(BufferType bufferType, BufferAllocator allocator, int bitmapIndex)
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
    protected void free0()
    {
        arena.free(chunkListNode, handle, memoryCapacity);
        arena         = null;
        chunkListNode = null;
        handle        = 0;
    }

    @Override
    protected void expansionCapacity(int newCapacity)
    {
        PooledBuffer2 newBuffer = (PooledBuffer2) allocator.ioBuffer(newCapacity);
        memoryCopy(memory, nativeAddress, offset, newBuffer.memory, newBuffer.nativeAddress, newBuffer.offset, writePosi);
        //当前在扩容，当前就不会slice，这两个不是并发的。因此refCount总是面对当前正确的memory
        free();
        init(newBuffer);
        this.arena         = newBuffer.arena;
        this.chunkListNode = newBuffer.chunkListNode;
        this.handle        = newBuffer.handle;
    }

    @Override
    public IoBuffer slice(int length)
    {
        int             oldReadPosi = nextReadPosi(length);
        PooledBuffer2 sliceBuffer = (PooledBuffer2) allocator.bufferInstance();
        sliceBuffer.init(this, oldReadPosi, length);
        sliceBuffer.arena = arena;
        sliceBuffer.chunkListNode = chunkListNode;
        sliceBuffer.handle = handle;
        return sliceBuffer;
    }
}
