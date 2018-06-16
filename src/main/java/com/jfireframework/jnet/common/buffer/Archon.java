package com.jfireframework.jnet.common.buffer;

public abstract class Archon
{
    private ChunkList      cDestory;
    private ChunkList      cInt;
    private ChunkList      c000;
    private ChunkList      c25;
    private ChunkList      c50;
    private ChunkList      c75;
    private ChunkList      c100;
    private int            maxLevel;
    private int            unit;
    protected final int    maxSize;
    protected ArchonMetric metric = new ArchonMetric();
    
    protected Archon(int maxLevel, int unit)
    {
        this.maxLevel = maxLevel;
        this.unit = unit;
        // c100不具备next节点，c25不具备prev节点。
        c100 = new ChunkList(null, Integer.MAX_VALUE, 90, "c100");
        c75 = new ChunkList(c100, 100, 75, "c075");
        c50 = new ChunkList(c75, 100, 50, "c050");
        c25 = new ChunkList(c50, 75, 25, "c025");
        c000 = new ChunkList(c25, 50, 0, "c000");
        cInt = new ChunkList(c000, 25, Integer.MIN_VALUE, "cInt");
        // 在列表的存在只是为了存储使用率为0的chunk。所以上限阀值应该尽可能低，使得chunk再次被激活时立刻进入其他的列表
        cDestory = new ChunkStoreList(c000, 1, Integer.MIN_VALUE, "");
        c000.setPrev(cDestory);
        c25.setPrev(c000);
        c50.setPrev(c25);
        c75.setPrev(c50);
        c100.setPrev(c75);
        maxSize = unit * (1 << (maxLevel));
    }
    
    public ArchonMetric metric()
    {
        return metric;
    }
    
    public synchronized void apply(PooledIoBuffer buffer, int need)
    {
        if (need > maxSize)
        {
            initHugeBuffer(buffer, need);
            return;
        }
        applyFromChunk(need, buffer, false);
    }
    
    protected final void applyFromChunk(int need, PooledIoBuffer buffer, boolean expansion)
    {
        int step = 0;
        if (//
        (step++ > 0 && c50.findChunkAndApply(need, buffer, expansion))//
                || (step++ > 0 && c25.findChunkAndApply(need, buffer, expansion))//
                || (step++ > 0 && c000.findChunkAndApply(need, buffer, expansion)) //
                || (step++ > 0 && cInt.findChunkAndApply(need, buffer, expansion))//
                || (step++ > 0 && c75.findChunkAndApply(need, buffer, expansion))//
                || (step++ > 0 && cDestory.findChunkAndApply(need, buffer, expansion))//
        )
        {
            metric.hitChunkStep(step);
            return;
        }
        metric.newChunkRecord();
        Chunk chunk = newChunk(maxLevel, unit);
        chunk.archon = this;
        // 先申请，后添加
        chunk.apply(need, buffer, expansion);
        cInt.addChunk(chunk);
    }
    
    public synchronized void recycle(Chunk chunk, int index)
    {
        ChunkList list = chunk.parent();
        if (list != null)
        {
            list.recycle(chunk, index);
        }
        else
        {
            throw new IllegalStateException("不应该出现chunk已经没有归属的list时还能出现回收的情况。因为一个chunk不属于一个list只可能是因为其使用率为0被移除");
        }
    }
    
    public synchronized void recycle(Chunk[] chunks, int[] indexs, int off, int len)
    {
        int end = off + len;
        for (int i = off; i < end; i++)
        {
            Chunk chunk = chunks[i];
            int index = indexs[i];
            ChunkList chunkList = chunk.parent;
            if (chunkList != null)
            {
                chunkList.recycle(chunk, index);
            }
            else
            {
                throw new IllegalStateException("不应该出现chunk已经没有归属的list时还能出现回收的情况。因为一个chunk不属于一个list只可能是因为其使用率为0被移除");
            }
        }
    }
    
    /**
     * 设置巨大内存区域的Buffer数据。该Buffer是没有关联chunk的
     * 
     * @param buffer
     * @param need
     */
    protected abstract void initHugeBuffer(PooledIoBuffer buffer, int need);
    
    protected abstract Chunk newChunk(int maxLevel, int unit);
    
    public synchronized void expansion(PooledIoBuffer buffer, int newSize)
    {
        if (newSize <= maxSize)
        {
            Chunk predChunk = buffer.chunk();
            int predIndex = buffer.index;
            applyFromChunk(newSize, buffer, true);
            if (predChunk != null)
            {
                recycle(predChunk, predIndex);
            }
        }
        else
        {
            expansionForHugeCapacity(buffer, newSize);
        }
    }
    
    public abstract boolean isDirect();
    
    protected abstract void expansionForHugeCapacity(PooledIoBuffer buffer, int newSize);
    
    public static Archon directPooledArchon(int maxLevel, int unit)
    {
        return new PooledDirectArchon(maxLevel, unit);
    }
    
    public static Archon heapPooledArchon(int maxLevel, int unit)
    {
        return new PooledHeapArchon(maxLevel, unit);
    }
}
