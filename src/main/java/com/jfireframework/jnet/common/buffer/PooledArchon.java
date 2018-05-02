package com.jfireframework.jnet.common.buffer;

public abstract class PooledArchon implements Archon
{
    private ChunkList cInt;
    private ChunkList c000;
    private ChunkList c25;
    private ChunkList c50;
    private ChunkList c75;
    private ChunkList c100;
    private int       maxLevel;
    private int       unit;
    protected int     maxSize;
    
    protected PooledArchon(int maxLevel, int unit)
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
        c25.setPrev(c000);
        c50.setPrev(c25);
        c75.setPrev(c50);
        c100.setPrev(c75);
        maxSize = unit * (1 << (maxLevel));
    }
    
    public String statistics()
    {
        StringBuilder builder = new StringBuilder();
        builder.append("cInt:").append(cInt.getStatistics().toString()).append(",").append("\r\n")//
                .append("c000:").append(c000.getStatistics().toString()).append(",").append("\r\n")//
                .append("c25:").append(c25.getStatistics().toString()).append(",").append("\r\n")//
                .append("c50:").append(c50.getStatistics().toString()).append(",").append("\r\n")//
                .append("c75:").append(c75.getStatistics().toString()).append(",").append("\r\n")//
                .append("c100:").append(c100.getStatistics().toString());//
        return builder.toString();
    }
    
    @Override
    public synchronized void apply(int need, PooledIoBuffer buffer)
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
        if (c50.findChunkAndApply(need, buffer, expansion)//
                || c25.findChunkAndApply(need, buffer, expansion) //
                || c000.findChunkAndApply(need, buffer, expansion) //
                || cInt.findChunkAndApply(need, buffer, expansion) //
                || c75.findChunkAndApply(need, buffer, expansion) //
        )
        {
            return;
        }
        Chunk chunk = newChunk(maxLevel, unit);
        chunk.archon = this;
        // 先申请，后添加
        chunk.apply(need, buffer, expansion);
        cInt.addChunk(chunk);
    }
    
    @Override
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
    
    /**
     * 设置巨大内存区域的Buffer数据。该Buffer是没有关联chunk的
     * 
     * @param buffer
     * @param need
     */
    protected abstract void initHugeBuffer(PooledIoBuffer buffer, int need);
    
    protected abstract Chunk newChunk(int maxLevel, int unit);
    
    @Override
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
    
    protected abstract void expansionForHugeCapacity(PooledIoBuffer buffer, int newSize);
    
    public static PooledArchon directPooledArchon(int maxLevel, int unit)
    {
        return new PooledDirectArchon(maxLevel, unit);
    }
    
    public static PooledArchon heapPooledArchon(int maxLevel, int unit)
    {
        return new PooledHeapArchon(maxLevel, unit);
    }
}
