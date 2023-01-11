package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.SubPage;
import com.jfirer.jnet.common.util.CapacityStat;

public class ChunkList<T>
{
    final int   maxUsage;
    final int   minUsage;
    final int   maxReqCapacity;
    final Arena arena;
    ChunkList<T>     prevList;
    ChunkList<T>     nextList;
    ChunkListNode<T> head;

    /**
     * 两个边界都是闭区间。也就是大于等于最小使用率，小于等于最大使用率都在这个List中
     *
     * @param minUsage
     * @param maxUsage
     * @param next
     * @param chunkSize
     */
    public ChunkList(int minUsage, int maxUsage, ChunkList<T> next, int chunkSize, Arena arena)
    {
        this.maxUsage = maxUsage;
        this.minUsage = minUsage;
        maxReqCapacity = calcuteMaxCapacity(minUsage, chunkSize);
        this.nextList = next;
        this.arena = arena;
    }

    int calcuteMaxCapacity(int minUsage, int chunkSize)
    {
        if (minUsage < 0)
        {
            return chunkSize;
        }
        else if (minUsage > 100)
        {
            return 0;
        }
        else
        {
            return (100 - minUsage) * chunkSize / 100;
        }
    }

    public void setPrevList(ChunkList<T> prevList)
    {
        this.prevList = prevList;
    }

    public Chunk.MemoryArea<T> allocate(int normalizeSize)
    {
        if (head == null || normalizeSize > maxReqCapacity)
        {
            return null;
        }
        ChunkListNode node = head;
        do
        {
            Chunk.MemoryArea<T> allocate = node.allocate(normalizeSize);
            if (allocate != null)
            {
                int usage = node.usage();
                if (usage > maxUsage)
                {
                    remove(node);
                    nextList.addFromPrev(node, usage);
                }
                return allocate;
            }
        }
        while ((node = node.getNext()) != null);
        return null;
    }

    public SubPageListNode allocateSubpage()
    {
        if (head == null)
        {
            return null;
        }
        ChunkListNode<T> node = head;
        do
        {
            SubPage subPage = node.allocateSubpage();
            if (subPage != null)
            {
                int usage = node.usage();
                if (usage > maxUsage)
                {
                    remove(node);
                    nextList.addFromPrev(node, usage);
                }
                return node.find(subPage.index());
            }
        }
        while ((node = node.getNext()) != null);
        return null;
    }

    /**
     * 返回true意味着该Chunk不在管理之中，可以销毁
     *
     * @param node
     * @param handle
     * @return
     */
    public boolean free(ChunkListNode<T> node, int handle)
    {
        node.free(handle);
        int usage = node.usage();
        if (usage < minUsage)
        {
            remove(node);
            return addFromNext(node, usage) == false;
        }
        return false;
    }

    void remove(ChunkListNode<T> node)
    {
        ChunkListNode<T> head = this.head;
        if (node == head)
        {
            head = node.getNext();
            if (head != null)
            {
                head.setPrev(null);
            }
            this.head = head;
        }
        else
        {
            ChunkListNode next = node.getNext();
            node.getPrev().setNext(next);
            if (next != null)
            {
                next.setPrev(node.getPrev());
            }
        }
    }

    /**
     * 返回true意味着有前向列表可以添加，否则返回false
     *
     * @param node
     * @param usage
     * @return
     */
    boolean addFromNext(ChunkListNode<T> node, int usage)
    {
        if (usage < minUsage)
        {
            if (prevList == null)
            {
                return false;
            }
            else
            {
                return prevList.addFromNext(node, usage);
            }
        }
        add(node);
        return true;
    }

    public void add(ChunkListNode<T> node)
    {
        node.setParent(this);
        if (head == null)
        {
            head = node;
            node.setPrev(null);
            node.setNext(null);
        }
        else
        {
            node.setPrev(null);
            node.setNext(head);
            head.setPrev(node);
            head = node;
        }
    }

    void addFromPrev(ChunkListNode node, int usage)
    {
        if (usage > maxUsage)
        {
            nextList.addFromPrev(node, usage);
            return;
        }
        add(node);
    }

    public void stat(CapacityStat stat)
    {
        ChunkListNode<T> cursor = head;
        if (cursor == null)
        {
            return;
        }
        stat.add(cursor);
        while ((cursor = cursor.getNext()) != null)
        {
            stat.add(cursor);
        }
    }

    public Arena getArena()
    {
        return arena;
    }

    public ChunkListNode<T> head()
    {
        return head;
    }
}
