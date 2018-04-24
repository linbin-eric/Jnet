package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.Statistics;

public class ChunkList
{
    private Chunk      head;
    private ChunkList  prev;
    private ChunkList  next;
    private int        maxUsage;
    private int        minUsage;
    private String     name;
    private Statistics statistics = new Statistics(2, 4, 6, 8, Integer.MAX_VALUE);
    
    public ChunkList(ChunkList next, int maxUsage, int minUsage, String name)
    {
        this.name = name;
        this.next = next;
        this.maxUsage = maxUsage;
        this.minUsage = minUsage;
    }
    
    @Override
    public String toString()
    {
        return name;
    }
    
    public void setPrev(ChunkList prev)
    {
        this.prev = prev;
    }
    
    public boolean findChunkAndApply(int need, AbstractIoBuffer bucket, Archon archon)
    {
        if (head == null)
        {
            return false;
        }
        int count = 1;
        Chunk select = head;
        boolean apply = false;
        while (select != null)
        {
            apply = select.apply(need, bucket, archon);
            if (apply == false)
            {
                select = select.next;
            }
            else
            {
                break;
            }
            count += 1;
        }
        statistics.count(count);
        if (apply && select.usage() >= maxUsage)
        {
            moveToNext(select);
        }
        return apply;
    }
    
    private void moveToNext(Chunk chunk)
    {
        removeFromCurrentList(chunk);
        next.addChunk(chunk);
    }
    
    private void moveToPrev(Chunk chunk)
    {
        removeFromCurrentList(chunk);
        if (prev != null)
        {
            prev.addChunk(chunk);
        }
    }
    
    private void removeFromCurrentList(Chunk node)
    {
        if (node == head)
        {
            head = head.next;
            if (head != null)
            {
                head.pred = null;
            }
        }
        else
        {
            Chunk pred = node.pred;
            Chunk next = node.next;
            pred.next = next;
            if (next != null)
            {
                next.pred = pred;
            }
        }
        node.parent = null;
        node.pred = null;
        node.next = null;
    }
    
    public void addChunk(Chunk chunk)
    {
        if (chunk.usage() > maxUsage)
        {
            next.addChunk(chunk);
            return;
        }
        if (head == null)
        {
            head = chunk;
        }
        else
        {
            chunk.next = head;
            head.pred = chunk;
            head = chunk;
        }
        chunk.parent = this;
    }
    
    public void recycle(IoBuffer handler)
    {
        Chunk chunk = handler.belong();
        chunk.recycle(handler);
        if (chunk.usage() <= minUsage)
        {
            moveToPrev(chunk);
        }
    }
    
    public Chunk head()
    {
        return head;
    }
    
    public Statistics getStatistics()
    {
        return statistics;
    }
}
