package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Chunk;

public class ChunkListNode<T> implements Chunk<T>
{
    private final SubPage<T>[]     subPages;
    private final Chunk<T>         delegation;
    private final int              pageSize;
    private final int              subPageIdxMask;
    private       ChunkList<T>     parent;
    private       ChunkListNode<T> prev;
    private       ChunkListNode<T> next;

    public ChunkListNode(ChunkList<T> parent, Chunk<T> chunk)
    {
        this.parent = parent;
        delegation = chunk;
        pageSize = chunk.pageSize();
        subPageIdxMask = 1 << chunk.maxLevle();
        subPages = new SubPage[1 << chunk.maxLevle()];
    }

    public ChunkListNode(Chunk<T> chunk)
    {
        if (chunk.isUnPooled() == false)
        {
            throw new IllegalArgumentException();
        }
        delegation = chunk;
        parent = null;
        pageSize = chunk.pageSize();
        subPageIdxMask = 0;
        subPages = null;
    }

    public SubPage<T> allocateSubPage(int normalizeCapacity)
    {
        if (delegation.isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        MemoryArea<T> memoryArea = delegation.allocate(pageSize);
        int           subPageIdx = subPageIdx(memoryArea.handle());
        SubPage<T>    subPage    = subPages[subPageIdx];
        if (subPage == null)
        {
            subPages[subPageIdx] = subPage = new SubPage<T>(this, pageSize, memoryArea.handle(), memoryArea.offset());
        }
        subPage.reset(normalizeCapacity);
        return subPage;
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    public SubPage<T> find(int subPageIdx)
    {
        if (delegation.isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        return subPages[subPageIdx];
    }

    public ChunkList<T> getParent()
    {
        if (delegation.isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        return parent;
    }

    public void setParent(ChunkList<T> parent)
    {
        this.parent = parent;
    }

    public ChunkListNode<T> getPrev()
    {
        if (delegation.isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        return prev;
    }

    public void setPrev(ChunkListNode<T> prev)
    {
        if (delegation.isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        this.prev = prev;
    }

    public ChunkListNode<T> getNext()
    {
        return next;
    }

    public void setNext(ChunkListNode<T> next)
    {
        this.next = next;
    }

    public Chunk<T> getChunk()
    {
        return delegation;
    }

    @Override
    public int usage()
    {
        return delegation.usage();
    }

    @Override
    public int getFreeBytes()
    {
        return delegation.getFreeBytes();
    }

    @Override
    public int getChunkSize()
    {
        return delegation.getChunkSize();
    }

    @Override
    public int pageSize()
    {
        return delegation.pageSize();
    }

    @Override
    public int maxLevle()
    {
        return delegation.maxLevle();
    }

    @Override
    public MemoryArea<T> allocate(int normalizeCapacity)
    {
        return delegation.allocate(normalizeCapacity);
    }

    @Override
    public void free(int handle)
    {
        delegation.free(handle);
    }

    @Override
    public T memory()
    {
        return delegation.memory();
    }

    @Override
    public long directChunkAddress()
    {
        return delegation.directChunkAddress();
    }

    @Override
    public boolean isUnPooled()
    {
        return delegation.isUnPooled();
    }
}
