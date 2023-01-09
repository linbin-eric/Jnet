package com.jfirer.jnet.common.buffer;

public class SubPageListNode
{
    private final SubPage         subPage;
    private final ChunkListNode   node;
    private       SubPageListNode prev;
    private       SubPageListNode next;

    public SubPageListNode(SubPage subPage, ChunkListNode node)
    {
        this.subPage = subPage;
        this.node = node;
    }

    public ChunkListNode getChunkListNode()
    {
        return node;
    }

    public SubPageListNode()
    {
        subPage = null;
        node = null;
        prev = next = this;
    }

    public SubPage getSubPage()
    {
        return subPage;
    }

    public SubPageListNode getPrev()
    {
        return prev;
    }

    public void setPrev(SubPageListNode prev)
    {
        this.prev = prev;
    }

    public SubPageListNode getNext()
    {
        return next;
    }

    public void setNext(SubPageListNode next)
    {
        this.next = next;
    }
}
