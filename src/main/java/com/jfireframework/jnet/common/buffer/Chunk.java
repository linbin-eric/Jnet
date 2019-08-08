package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.MathUtil;

public abstract class Chunk<T>
{
    protected final int          pageSizeShift;
    protected final int          subpageOverflowMask;
    protected final int          maxLevel;
    protected final int[]        allocationCapacity;
    protected final int          chunkSize;
    protected final T            memory;
    protected       int          freeBytes;
    protected       boolean      unpooled = false;
    protected       SubPage<T>[] subPages;
    protected       int          subPageIdxMask;
    protected       int          pageSize;
    protected       Arena<T>     arena;
    /* 供ChunkList使用 */
    protected       ChunkList<T> parent;
    protected       Chunk<T>     next;
    protected       Chunk<T>     pred;
    /* 供ChunkList使用 */

    /**
     * 初始化一个chunk。
     *
     * @param maxLevel 最大层次。起始层次为0。
     * @param pageSize 单页字节大小。也就是一个最小的分配区域的字节数。
     */
    public Chunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        this.maxLevel = maxLevel;
        this.pageSize = pageSize;
        this.pageSizeShift = pageSizeShift;
        this.subpageOverflowMask = subpageOverflowMask;
        allocationCapacity = new int[1 << (maxLevel + 1)];
        subPageIdxMask = 1 << maxLevel;
        for (int i = 0; i <= maxLevel; i++)
        {
            int initializeSize = calculateSize(i);
            int start          = 1 << i;
            int end            = (1 << (i + 1));
            for (int j = start; j < end; j++)
            {
                allocationCapacity[j] = initializeSize;
            }
        }
        freeBytes = allocationCapacity[1];
        chunkSize = freeBytes;
        memory = initializeMemory(chunkSize);
    }

    /**
     * 非池化的特殊Chunk
     */
    public Chunk(int chunkSize)
    {
        unpooled = true;
        this.chunkSize = chunkSize;
        memory = initializeMemory(chunkSize);
        maxLevel = 0;
        pageSizeShift = 0;
        allocationCapacity = null;
        subpageOverflowMask = 0;
    }

    private int calculateSize(int level)
    {
        return 1 << (maxLevel - level + pageSizeShift);
    }

    private int calculateSizeShift(int level)
    {
        return (maxLevel - level + pageSizeShift);
    }

    abstract T initializeMemory(int size);

    /**
     * 该chunk是否使用堆外内存
     *
     * @return
     */
    public abstract boolean isDirect();

    @SuppressWarnings("unchecked")
    public long allocate(int normalizeSize)
    {
        if (allocationCapacity[1] < normalizeSize)
        {
            return -1;
        }
        if (isTinyOrSmall(normalizeSize))
        {
            int allocationsCapacityIdx = allocateNode(pageSize);
            if (allocationsCapacityIdx == -1)
            {
                return -1;
            }
            int        subPageIdx = subPageIdx(allocationsCapacityIdx);
            SubPage<T> head       = arena.findSubPageHead(normalizeSize);
            synchronized (head)
            {
                if (subPages == null)
                {
                    subPages = new SubPage[1 << maxLevel];
                }
                SubPage<T> subPage = subPages[subPageIdx];
                if (subPage != null)
                {
                    subPage.init(normalizeSize, arena);
                }
                else
                {
                    subPage = new SubPage<>(this, pageSize, allocationsCapacityIdx, subPageIdx << pageSizeShift, normalizeSize, arena);
                    subPages[subPageIdx] = subPage;
                }
                return subPage.allocate();
            }
        }
        else
        {
            return allocateNode(normalizeSize);
        }
    }

    private int allocateNode(int normalizeSize)
    {
        freeBytes -= normalizeSize;
        int hitLevel = calcuteLevel(normalizeSize);
        int index    = findNode(normalizeSize, hitLevel);
        allocationCapacity[index] = 0;
        updateAllocatedParent(index);
        return index;
    }

    private int calcuteLevel(int normalizeSize)
    {
        return maxLevel - (MathUtil.log2(normalizeSize) - pageSizeShift);
    }

    protected void initBuf(long handle, PooledBuffer<T> buffer, ThreadCache cache)
    {
        int bitMapIdx = bitmapIdx(handle);
        if (bitMapIdx == 0)
        {
            initNormalizeSIzeBuffer(handle, buffer, cache);
        }
        else
        {
            initSubPageBuffer(handle, bitMapIdx & 0x3FFFFFFF, buffer, cache);
        }
    }

    protected void unPoooledChunkInitBuf(PooledBuffer<T> buffer, ThreadCache cache)
    {
        buffer.initUnPooled(this, cache);
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    private int bitmapIdx(long handle)
    {
        return (int) (handle >>> 32);
    }

    int allocationsCapacityIdx(long handle)
    {
        return (int) handle;
    }

    private void initNormalizeSIzeBuffer(long handle, PooledBuffer<T> buffer, ThreadCache cache)
    {
        int allocationsCapacityIdx = (int) handle;
        int level                  = MathUtil.log2(allocationsCapacityIdx);
        int capacityShift          = calculateSizeShift(level);
        /**
         * 1<<hitLevel得到是该层节点数量，同时也是该层第一个节点的下标，为2的次方幂。<br/>
         * 与index进行异或操作就可以去掉最高位的1，也就是得到了index与该值的差。
         */
        int off = (allocationsCapacityIdx ^ (1 << level)) << capacityShift;
        buffer.init(this, 1 << capacityShift, off, handle, cache);
    }

    private int calcuteOffset(int allocationsCapacityIdx)
    {
        int level         = MathUtil.log2(allocationsCapacityIdx);
        int capacityShift = calculateSizeShift(level);
        /**
         * 1<<hitLevel得到是该层节点数量，同时也是该层第一个节点的下标，为2的次方幂。<br/>
         * 与index进行异或操作就可以去掉最高位的1，也就是得到了index与该值的差。
         */
        return (allocationsCapacityIdx ^ (1 << level)) << capacityShift;
    }

    private void initSubPageBuffer(long handle, int bitmapIdx, PooledBuffer<T> buffer, ThreadCache cache)
    {
        int        allocationsCapacityIdx = allocationsCapacityIdx(handle);
        int        subPageIdx             = subPageIdx(allocationsCapacityIdx);
        SubPage<T> subPage                = subPages[subPageIdx];
        int        offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPage.elementSize;
        buffer.init(this, subPage.elementSize, offset, handle, cache);
    }

    /**
     * 回收handle处的的空间
     *
     * @param handle
     */
    public void free(long handle)
    {
        int bitMapIdx              = bitmapIdx(handle);
        int allocationsCapacityIdx = allocationsCapacityIdx(handle);
        if (bitMapIdx > 0)
        {
            SubPage<T> subPage = subPages[subPageIdx(allocationsCapacityIdx)];
            SubPage<T> head    = arena.findSubPageHead(subPage.elementSize);
            synchronized (head)
            {
                if (subPage.free(handle, bitMapIdx & 0x3FFFFFFF, head, arena))
                {
                    return;
                }
            }
        }
        freeNode(allocationsCapacityIdx);
    }

    private void freeNode(int allocationsCapacityIdx)
    {
        int level     = MathUtil.log2(allocationsCapacityIdx);
        int levelSize = calculateSize(level);
        freeBytes += levelSize;
        allocationCapacity[allocationsCapacityIdx] = levelSize;
        int index = allocationsCapacityIdx;
        while (index > 1)
        {
            int parentIndex = index >> 1;
            int value       = allocationCapacity[index];
            int value2      = allocationCapacity[index ^ 1];
            levelSize = calculateSize(level);
            if (value == levelSize && value2 == levelSize)
            {
                allocationCapacity[parentIndex] = levelSize << 1;
            }
            else
            {
                allocationCapacity[parentIndex] = value > value2 ? value : value2;
            }
            index = parentIndex;
            level -= 1;
        }
    }

    private int findNode(int normalizeSize, int hitLevel)
    {
        int childIndex = 1;
        int level      = 0;
        while (level < hitLevel)
        {
            childIndex <<= 1;
            if (allocationCapacity[childIndex] < normalizeSize)
            {
                childIndex ^= 1;
            }
            level += 1;
        }
        return childIndex;
    }

    private void updateAllocatedParent(int index)
    {
        // 设置当前节点最大可分配为0
        while (index > 1)
        {
            int parentIndex = index >> 1;
            int value       = allocationCapacity[index];
            int value2      = allocationCapacity[index ^ 1];
            int parentValue = value > value2 ? value : value2;
            allocationCapacity[parentIndex] = parentValue;
            index = parentIndex;
        }
    }

    public int usage()
    {
        int result = 100 - (freeBytes * 100 / chunkSize);
        if (result == 0)
        {
            return freeBytes == chunkSize ? 0 : 1;
        }
        else if (result == 100)
        {
            return freeBytes == 0 ? 100 : 99;
        }
        else
        {
            return result;
        }
    }

    private boolean isTinyOrSmall(int normCapacity)
    {
        return (normCapacity & subpageOverflowMask) == 0;
    }

    public int getChunkSize()
    {
        return chunkSize;
    }

    public int getFreeBytes()
    {
        return freeBytes;
    }
}
