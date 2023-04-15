package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.buffer.BufferType;

public class ChunkListNode extends Chunk
{
    private final SubPage[]     subPages;
    private final int           subPageIdxMask;
    private       ChunkList     parent;
    private       ChunkListNode prev;
    private       ChunkListNode next;

    public ChunkListNode(ChunkList parent, int maxLevel, int pageSize, BufferType bufferType)
    {
        super(maxLevel, pageSize, bufferType);
        this.parent = parent;
        subPageIdxMask = 1 << maxLevel;
        subPages = new SubPage[1 << maxLevel];
    }

    public ChunkListNode(int chunkSize, BufferType bufferType)
    {
        super(chunkSize, bufferType);
        parent = null;
        subPageIdxMask = 0;
        subPages = null;
    }

    public SubPage allocateSubPage(int normalizeCapacity)
    {
        if (isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        MemoryArea memoryArea = allocate(pageSize);
        int        subPageIdx = subPageIdx(memoryArea.handle());
        SubPage    subPage    = subPages[subPageIdx];
        if (subPage == null)
        {
            subPages[subPageIdx] = subPage = new SubPage(this, pageSize, memoryArea.handle(), memoryArea.offset());
        }
        subPage.reset(normalizeCapacity);
        return subPage;
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    public SubPage find(int subPageIdx)
    {
        if (isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        return subPages[subPageIdx];
    }

    public ChunkList getParent()
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        return parent;
    }

    public void setParent(ChunkList parent)
    {
        this.parent = parent;
    }

    public ChunkListNode getPrev()
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        return prev;
    }

    public void setPrev(ChunkListNode prev)
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        this.prev = prev;
    }

    public ChunkListNode getNext()
    {
        return next;
    }

    public void setNext(ChunkListNode next)
    {
        this.next = next;
    }
}
