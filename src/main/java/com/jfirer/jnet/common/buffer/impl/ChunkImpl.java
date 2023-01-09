package com.jfirer.jnet.common.buffer.impl;

import com.jfirer.jnet.common.buffer.Chunk;
import com.jfirer.jnet.common.buffer.PooledBuffer;
import com.jfirer.jnet.common.buffer.SubPage;
import com.jfirer.jnet.common.buffer.ThreadCache;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public abstract class ChunkImpl<T> implements Chunk<T>
{
    //PageSize的log2的结果值。1<<pageSizeShift=PageSize。用于各类大小的计算。
    protected final int          pageSizeShift;
    //用于快速判断申请大小是否小于PageSize
    protected final int          subPageOverflowMask;
    //内存块管理的二叉树的深度，根节点深度为0.
    protected final int          maxLevel;
    //管理内存的二叉树
    protected final int[]        allocationCapacity;
    protected final MemoryArea[] memoryAreas;
    protected final SubPage[]    subPages;
    protected final int          chunkSize;
    protected final T            memory;
    protected final int          pageSize;
    protected final boolean      unpooled;
    protected final long         directBufferAddress;
    //二叉树最左叶子节点的下标值。该值与叶子节点下标相与，可快速得到叶子节点对应的SubPage对象，在SubPage[]中的下标值。
    protected final int          subPageIdxMask;
    protected       int          freeBytes;

    /* 供ChunkList使用 */
    public ChunkImpl(int maxLevel, int pageSize)
    {
        this.pageSize = pageSize;
        this.maxLevel = maxLevel;
        pageSizeShift = MathUtil.log2(pageSize);
        subPageOverflowMask = ~(pageSize - 1);
        Data data = initAllocationCapacityAndMemoryAreas(maxLevel);
        allocationCapacity = data.allocationCapacity();
        memoryAreas = data.memoryAreas();
        freeBytes = chunkSize = 1 << (maxLevel + pageSizeShift);
        subPageIdxMask = 1 << maxLevel;
        memory = initializeMemory(chunkSize);
        unpooled = false;
        subPages = new SubPage[1 << maxLevel];
        for (int i = 0; i < subPages.length; i++)
        {
            MemoryArea memoryArea = memoryAreas[1 << maxLevel + i];
            subPages[i] = SubPage.newSubPage(memoryArea.handle(), memoryArea.capacity(), memoryArea.offset(), memory, this);
        }
        if (memory instanceof ByteBuffer buffer && buffer.isDirect())
        {
            directBufferAddress = PlatFormFunction.bytebufferOffsetAddress(buffer);
        }
        else
        {
            directBufferAddress = 0;
        }
    }

    record Data(int[] allocationCapacity, MemoryArea[] memoryAreas) {}

    private Data initAllocationCapacityAndMemoryAreas(int maxLevel)
    {
        int[]        allocationCapacity = new int[1 << (maxLevel + 1)];
        MemoryArea[] memoryAreas        = new MemoryArea[allocationCapacity.length];
        for (int i = 0; i <= maxLevel; i++)
        {
            int initializeSize = calculateSize(i);
            int start          = 1 << i;
            int end            = (1 << (i + 1));
            for (int j = start; j < end; j++)
            {
                allocationCapacity[j] = initializeSize;
                memoryAreas[j] = new MemoryArea(j, initializeSize, calcuteOffset(j), memory, this);
            }
        }
        return new Data(allocationCapacity, memoryAreas);
    }

    /**
     * 非池化的特殊Chunk
     */
    public ChunkImpl(int chunkSize)
    {
        unpooled = true;
        this.chunkSize = chunkSize;
        memory = initializeMemory(chunkSize);
        maxLevel = 0;
        pageSizeShift = 0;
        allocationCapacity = null;
        subPageOverflowMask = 0;
        pageSize = 0;
        subPageIdxMask = 0;
        memoryAreas = null;
        subPages = null;
        if (memory instanceof ByteBuffer buffer && buffer.isDirect())
        {
            directBufferAddress = PlatFormFunction.bytebufferOffsetAddress(buffer);
        }
        else
        {
            directBufferAddress = 0;
        }
    }

    private int calculateSize(int level)
    {
        return 1 << (maxLevel - level + pageSizeShift);
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

    private int calculateSizeShift(int level)
    {
        return (maxLevel - level + pageSizeShift);
    }

    protected abstract T initializeMemory(int size);

    /**
     * 该chunk是否使用堆外内存
     *
     * @return
     */
    public abstract boolean isDirect();

    @Override
    public MemoryArea<T> allocate(int normalizeSize)
    {
        if (allocationCapacity[1] < normalizeSize)
        {
            return null;
        }
        return allocateNode(normalizeSize);
//        if (isTinyOrSmall(normalizeSize))
//        {
//            int allocationsCapacityIdx = allocateNode(pageSize);
//            if (allocationsCapacityIdx == -1)
//            {
//                return -1;
//            }
//            int            subPageIdx = subPageIdx(allocationsCapacityIdx);
//            SubPageImpl<T> head       = arena.findSubPageHead(normalizeSize);
//            synchronized (head)
//            {
//                if (subPages == null)
//                {
//                    subPages = new SubPageImpl[1 << maxLevel];
//                }
//                SubPageImpl<T> subPage = subPages[subPageIdx];
//                if (subPage != null)
//                {
//                    subPage.init(normalizeSize, arena);
//                }
//                else
//                {
//                    subPage = new SubPageImpl<>(this, pageSize, allocationsCapacityIdx, subPageIdx << pageSizeShift, normalizeSize, arena);
//                    subPages[subPageIdx] = subPage;
//                }
//                return subPage.allocate();
//            }
//        }
//        else
//        {
//        }
    }

    @Override
    public SubPage allocateSubpage()
    {
        MemoryArea<T> allocate = allocate(pageSize);
        if (allocate == null)
        {
            return null;
        }
        return subPages[subPageIdx(allocate.handle())];
    }

    private MemoryArea<T> allocateNode(int normalizeSize)
    {
        freeBytes -= normalizeSize;
        int hitLevel = calcuteLevel(normalizeSize);
        int index    = findNode(normalizeSize, hitLevel);
        allocationCapacity[index] = 0;
        updateAllocatedParent(index);
        return memoryAreas[index];
    }

    private int calcuteLevel(int normalizeSize)
    {
        return maxLevel - (MathUtil.log2(normalizeSize) - pageSizeShift);
    }
//    protected void initBuf(long handle, PooledBuffer<T> buffer, ThreadCache cache)
//    {
//        int bitMapIdx = bitmapIdx(handle);
//        if (bitMapIdx == 0)
//        {
//            initNormalizeSIzeBuffer(handle, buffer, cache);
//        }
//        else
//        {
//            initSubPageBuffer(handle, bitMapIdx & 0x3FFFFFFF, buffer, cache);
//        }
//    }

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
//    private void initNormalizeSIzeBuffer(long handle, PooledBuffer<T> buffer, ThreadCache cache)
//    {
//        int allocationsCapacityIdx = (int) handle;
//        int level                  = MathUtil.log2(allocationsCapacityIdx);
//        int capacityShift          = calculateSizeShift(level);
//        /**
//         * 1<<hitLevel得到是该层节点数量，同时也是该层第一个节点的下标，为2的次方幂。<br/>
//         * 与index进行异或操作就可以去掉最高位的1，也就是得到了index与该值的差。
//         */
//        int off = (allocationsCapacityIdx ^ (1 << level)) << capacityShift;
//        buffer.init(this, 1 << capacityShift, off, handle, cache);
//    }
//    private void initSubPageBuffer(long handle, int bitmapIdx, PooledBuffer<T> buffer, ThreadCache cache)
//    {
//        int            allocationsCapacityIdx = allocationsCapacityIdx(handle);
//        int            subPageIdx             = subPageIdx(allocationsCapacityIdx);
//        SubPageImpl<T> subPage                = subPages[subPageIdx];
//        int            offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPage.elementSize;
//        buffer.init(this, subPage.elementSize, offset, handle, cache);
//    }
//    /**
//     * 回收handle处的的空间
//     *
//     * @param handle
//     */
//    public void free(long handle)
//    {
//        int bitMapIdx              = bitmapIdx(handle);
//        int allocationsCapacityIdx = allocationsCapacityIdx(handle);
//        if (bitMapIdx > 0)
//        {
//            SubPageImpl<T> subPage = subPages[subPageIdx(allocationsCapacityIdx)];
//            SubPageImpl<T> head    = arena.findSubPageHead(subPage.elementSize);
//            synchronized (head)
//            {
//                if (subPage.free(handle, bitMapIdx & 0x3FFFFFFF, head, arena))
//                {
//                    return;
//                }
//            }
//        }
//        freeNode(allocationsCapacityIdx);
//    }

    @Override
    public void free(int handle)
    {
        freeNode(handle);
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

    @Override
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
        //该结果等同于 normCapacity<pagesize
        return (normCapacity & subPageOverflowMask) == 0;
    }

    @Override
    public int getChunkSize()
    {
        return chunkSize;
    }

    @Override
    public int getFreeBytes()
    {
        return freeBytes;
    }

    @Override
    public long directChunkAddress()
    {
        return directBufferAddress;
    }
}
