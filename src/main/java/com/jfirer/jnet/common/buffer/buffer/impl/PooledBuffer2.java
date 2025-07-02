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
public class PooledBuffer2 extends UnPooledBuffer2 implements ArenaAccepter
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
        newCapacity = newCapacity >= (bufferCapacity << 1) ? newCapacity : (bufferCapacity << 1);
        int           oldReadPosi       = readPosi;
        int           oldWritePosi      = writePosi;
        Arena         oldArena          = arena;
        ChunkListNode oldChunkListNode  = chunkListNode;
        long          oldHandle         = handle;
        int           oldOffset         = offset;
        int           oldBufferCapacity = bufferCapacity;
        Object        oldMemory         = memory;
        long          oldNativeAddress  = nativeAddress;
        int           oldMemoryCapacity = memoryCapacity;
        AtomicInteger oldRefCnt         = refCnt;
        allocator.reAllocate(newCapacity, this);
        memoryCopy(oldMemory, oldNativeAddress, oldOffset, this.memory, this.nativeAddress, this.offset, oldBufferCapacity);
        if (oldRefCnt.decrementAndGet() == 0)
        {
            //当前在扩容，当前就不会slice，这两个不是并发的。因此refCount总是面对当前正确的memory
            oldArena.free(oldChunkListNode, oldHandle, oldMemoryCapacity);
            oldRefCnt.incrementAndGet();
            this.refCnt = oldRefCnt;
        }
        else
        {
            this.refCnt = new AtomicInteger(1);
        }
        readPosi  = oldReadPosi;
        writePosi = oldWritePosi;
    }

    @Override
    public IoBuffer slice(int length)
    {
        int           oldReadPosi = nextReadPosi(length);
        PooledBuffer2 sliceBuffer = (PooledBuffer2) allocator.bufferInstance();
        sliceBuffer.init(this, oldReadPosi, length);
        sliceBuffer.arena         = arena;
        sliceBuffer.chunkListNode = chunkListNode;
        sliceBuffer.handle        = handle;
        return sliceBuffer;
    }

    @Override
    public IoBuffer compact()
    {
        if (refCnt.get() == 1)
        {
            if (readPosi == 0)
            {
                return this;
            }
            int length = remainRead();
            if (length == 0)
            {
                writePosi = readPosi = 0;
            }
            else
            {
                rwDelegation.compact0(memory, offset, nativeAddress, readPosi, length);
                readPosi  = 0;
                writePosi = length;
            }
        }
        else
        {
            int           length            = remainRead();
            int           oldReadPosi       = readPosi;
            Arena         oldArena          = arena;
            ChunkListNode oldChunkListNode  = chunkListNode;
            long          oldHandle         = handle;
            int           oldOffset         = offset;
            Object        oldMemory         = memory;
            long          oldNativeAddress  = nativeAddress;
            int           oldMemoryCapacity = memoryCapacity;
            AtomicInteger oldRefCnt         = refCnt;
            allocator.reAllocate(Math.max(16, length), this);
            if (length == 0)
            {
                ;
            }
            else
            {
                memoryCopy(oldMemory, oldNativeAddress, oldOffset + oldReadPosi, this.memory, this.nativeAddress, this.offset, length);
            }
            if (oldRefCnt.decrementAndGet() == 0)
            {
                oldArena.free(oldChunkListNode, oldHandle, oldMemoryCapacity);
                oldRefCnt.incrementAndGet();
                this.refCnt = oldRefCnt;
            }
            else
            {
                refCnt = new AtomicInteger(1);
            }
            readPosi  = 0;
            writePosi = length;
        }
        return this;
    }

    @Override
    public String toString()
    {
        return "PooledBuffer2{" + ", bitmapIndex=" + bitmapIndex + ", refCnt=" + refCnt;
    }
}
