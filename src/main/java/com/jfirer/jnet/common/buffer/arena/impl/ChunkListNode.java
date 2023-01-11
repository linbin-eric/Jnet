package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.SubPage;

public class ChunkListNode<T>
{
    private       ChunkList<T>      parent;
    private       ChunkListNode<T>  prev;
    private       ChunkListNode<T>  next;
    private final SubPageListNode[] subPageListNodes;
    private final Chunk<T>          chunk;

    public ChunkListNode(ChunkList<T> parent, Chunk<T> chunk)
    {
        this.parent = parent;
        this.chunk = chunk;
        subPageListNodes = new SubPageListNode[1 << chunk.maxLevle()];
    }

    public SubPageListNode exchange(SubPage subPage)
    {
        int             index           = subPage.index();
        SubPageListNode subPageListNode = subPageListNodes[index];
        if (subPageListNode == null)
        {
            subPageListNode = new SubPageListNode(subPage, this);
            subPageListNodes[index] = subPageListNode;
        }
        return subPageListNode;
    }

    public SubPageListNode find(int index)
    {
        SubPageListNode subPageListNode = subPageListNodes[index];
        if (subPageListNode == null)
        {
            throw new IllegalStateException("不应该为空");
        }
        return subPageListNode;
    }

    public ChunkList<T> getParent()
    {
        return parent;
    }

    public void setParent(ChunkList<T> parent)
    {
        this.parent = parent;
    }

    public ChunkListNode<T> getPrev()
    {
        return prev;
    }

    public void setPrev(ChunkListNode<T> prev)
    {
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
        return chunk;
    }
}
