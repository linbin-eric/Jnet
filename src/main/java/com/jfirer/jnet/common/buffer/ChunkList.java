package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.util.CapacityStat;

public class ChunkList<T>
{
    final int maxUsage;
    final int minUsage;
    final int maxReqCapacity;
    ChunkList<T> prevList;
    ChunkList<T> nextList;
    Chunk<T>     head;

    /**
     * 两个边界都是闭区间。也就是大于等于最小使用率，小于等于最大使用率都在这个List中
     *
     * @param minUsage
     * @param maxUsage
     * @param next
     * @param chunkSize
     */
    public ChunkList(int minUsage, int maxUsage, ChunkList<T> next, int chunkSize)
    {
        this.maxUsage = maxUsage;
        this.minUsage = minUsage;
        maxReqCapacity = calcuteMaxCapacity(minUsage, chunkSize);
        this.nextList = next;
    }

    public void setPrevList(ChunkList<T> prevList)
    {
        this.prevList = prevList;
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

    public boolean allocate(int normalizeSize, PooledBuffer<T> buffer, ThreadCache cache)
    {
        if (head == null || normalizeSize >= maxReqCapacity)
        {
            return false;
        }
        Chunk<T> cursor = head;
        long     handle = cursor.allocate(normalizeSize);
        if (handle != -1)
        {
            cursor.initBuf(handle, buffer, cache);
            int usage = cursor.usage();
            if (usage > maxUsage)
            {
                remove(cursor);
                nextList.addFromPrev(cursor, usage);
            }
            return true;
        }
        while ((cursor = cursor.next) != null)
        {
            handle = cursor.allocate(normalizeSize);
            if (handle != -1)
            {
                cursor.initBuf(handle, buffer, cache);
                return true;
            }
        }
        return false;
    }

    /**
     * 返回true意味着该Chunk不在管理之中，可以摧毁。
     *
     * @param chunk
     * @param handle
     * @return
     */
    public boolean free(Chunk<T> chunk, long handle)
    {
        chunk.free(handle);
        int usage = chunk.usage();
        if (usage < minUsage)
        {
            remove(chunk);
            return addFromNext(chunk, usage) == false;
        }
        return false;
    }

    void remove(Chunk<T> node)
    {
        Chunk<T> head = this.head;
        if (node == head)
        {
            head = node.next;
            if (head != null)
            {
                head.pred = null;
            }
            this.head = head;
        }
        else
        {
            Chunk<T> next = node.next;
            node.pred.next = next;
            if (next != null)
            {
                next.pred = node.pred;
            }
        }
    }

    /**
     * 返回true意味着有前向列表可以添加，否则返回false
     *
     * @param chunk
     * @param usage
     * @return
     */
    boolean addFromNext(Chunk<T> chunk, int usage)
    {
        if (usage < minUsage)
        {
            if (prevList == null)
            {
                return false;
            }
            else
            {
                return prevList.addFromNext(chunk, usage);
            }
        }
        add(chunk);
        return true;
    }

    void add(Chunk<T> chunk)
    {
        chunk.parent = this;
        if (head == null)
        {
            head = chunk;
            chunk.pred = null;
            chunk.next = null;
        }
        else
        {
            chunk.pred = null;
            chunk.next = head;
            head.pred = chunk;
            head = chunk;
        }
    }

    void addFromPrev(Chunk<T> chunk, int usage)
    {
        if (usage > maxUsage)
        {
            nextList.addFromPrev(chunk, usage);
            return;
        }
        add(chunk);
    }

    int sum()
    {
        Chunk<T> cursor = head;
        if (cursor == null)
        {
            return 0;
        }
        int count = 1;
        while ((cursor = cursor.next) != null)
        {
            count++;
        }
        return count;
    }

    public void stat(CapacityStat stat)
    {
        Chunk<T> cursor = head;
        if (cursor == null)
        {
            return;
        }
        stat.add(cursor);
        while ((cursor = cursor.next) != null)
        {
            stat.add(cursor);
        }
    }
}
