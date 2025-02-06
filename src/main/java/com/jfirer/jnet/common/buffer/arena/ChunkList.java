package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.buffer.storage.PooledStorageSegment;
import com.jfirer.jnet.common.util.CapacityStat;

public class ChunkList
{
    final int   maxUsage;
    final int   minUsage;
    final int   maxReqCapacity;
    final Arena arena;
    ChunkList     prevList;
    ChunkList     nextList;
    ChunkListNode head;

    /**
     * 两个边界都是闭区间。也就是大于等于最小使用率，小于等于最大使用率都在这个List中
     *
     * @param minUsage
     * @param maxUsage
     * @param next
     * @param chunkSize
     */
    public ChunkList(int minUsage, int maxUsage, ChunkList next, int chunkSize, Arena arena)
    {
        this.maxUsage  = maxUsage;
        this.minUsage  = minUsage;
        maxReqCapacity = calcuteMaxCapacity(minUsage, chunkSize);
        this.nextList  = next;
        this.arena     = arena;
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

    public void setPrevList(ChunkList prevList)
    {
        this.prevList = prevList;
    }

    public boolean allocate(int normalizeSize, PooledStorageSegment storageSegment)
    {
        if (head == null || normalizeSize > maxReqCapacity)
        {
            return false;
        }
        ChunkListNode node = head;
        do
        {
            Chunk.MemoryArea allocate = node.allocate(normalizeSize);
            if (allocate != null)
            {
                int usage = node.usage();
                if (usage > maxUsage)
                {
                    remove(node);
                    nextList.addFromPrev(node, usage);
                }
                storageSegment.init(arena, node, allocate.handle(), allocate.offset(), allocate.capacity());
                return true;
            }
        } while ((node = node.getNext()) != null);
        return false;
    }

    public SubPage allocateSubpage(int normalizeCapacity)
    {
        if (head == null)
        {
            return null;
        }
        ChunkListNode node = head;
        do
        {
            SubPage subPage = node.allocateSubPage(normalizeCapacity);
            if (subPage != null)
            {
                int usage = node.usage();
                if (usage > maxUsage)
                {
                    remove(node);
                    nextList.addFromPrev(node, usage);
                }
                return subPage;
            }
        } while ((node = node.getNext()) != null);
        return null;
    }

    /**
     * 返回true意味着该Chunk不在管理之中，可以销毁
     *
     * @param node
     * @param handle
     * @return
     */
    public boolean free(ChunkListNode node, int handle)
    {
        node.free(handle);
        int usage = node.usage();
        if (usage < minUsage)
        {
            remove(node);
            return !addFromNext(node, usage);
        }
        return false;
    }

    void remove(ChunkListNode node)
    {
        ChunkListNode head = this.head;
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
    boolean addFromNext(ChunkListNode node, int usage)
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

    public void add(ChunkListNode node)
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
        ChunkListNode cursor = head;
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

    public ChunkListNode head()
    {
        return head;
    }
}
