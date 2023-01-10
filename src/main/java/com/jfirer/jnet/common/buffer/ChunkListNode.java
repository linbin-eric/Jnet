package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;

public abstract class ChunkListNode<T> extends ChunkImpl<T>
{
    private ChunkList<T>      parent;
    private ChunkListNode<T>  prev;
    private ChunkListNode<T>  next;
    private SubPageListNode[] subPageListNodes;

    public ChunkListNode(int maxLevel, int pageSize, ChunkList<T> parent)
    {
        super(maxLevel, pageSize);
        this.parent = parent;
        subPageListNodes = new SubPageListNode[1 << maxLevel];
        for (int i = 0; i < subPageListNodes.length; i++)
        {
            subPageListNodes[i] = new SubPageListNode(subPages[i], this);
        }
    }


    public SubPageListNode find(int index)
    {
        return subPageListNodes[index];
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
}
