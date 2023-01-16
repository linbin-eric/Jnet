package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public abstract class ChunkImpl<T> implements Chunk<T>
{
    //内存块管理的二叉树的深度，根节点深度为0.
    protected final int              maxLevel;
    //管理内存的二叉树
    protected final MemoryTreeNode[] memoryTree;
    protected final int              pageSize;
    //PageSize的log2的结果值。1<<pageSizeShift=PageSize。用于各类大小的计算。
    protected final int              pageSizeShift;
    //用于快速判断申请大小是否小于PageSize
    protected final int              subPageOverflowMask;
    protected final int              chunkSize;
    protected final T                memory;
    protected final boolean          unpooled;
    protected final long             directBufferAddress;
    protected       int              freeBytes;

    /* 供ChunkList使用 */
    public ChunkImpl(int maxLevel, int pageSize)
    {
        this.pageSize = pageSize;
        this.maxLevel = maxLevel;
        pageSizeShift = MathUtil.log2(pageSize);
        subPageOverflowMask = ~(pageSize - 1);
        freeBytes = chunkSize = 1 << (maxLevel + pageSizeShift);
        memory = initializeMemory(chunkSize);
        memoryTree = initMemoryTree(maxLevel);
        unpooled = false;
        if (memory instanceof ByteBuffer buffer && buffer.isDirect())
        {
            directBufferAddress = PlatFormFunction.bytebufferOffsetAddress(buffer);
        }
        else
        {
            directBufferAddress = 0;
        }
    }

    /**
     * 非池化的Chunk，用于chunkSize大于标准大小的Chunk，此时一个Chunk就是完整的内存区域供使用。
     */
    public ChunkImpl(int chunkSize)
    {
        unpooled = true;
        this.chunkSize = chunkSize;
        memory = initializeMemory(chunkSize);
        maxLevel = 0;
        pageSizeShift = 0;
        memoryTree = null;
        subPageOverflowMask = 0;
        pageSize = 0;
        if (memory instanceof ByteBuffer buffer && buffer.isDirect())
        {
            directBufferAddress = PlatFormFunction.bytebufferOffsetAddress(buffer);
        }
        else
        {
            directBufferAddress = 0;
        }
    }

    protected MemoryTreeNode[] initMemoryTree(int maxLevel)
    {
        MemoryTreeNode[] memoryTree = new MemoryTreeNode[1 << (maxLevel + 1)];
        for (int i = 0; i <= maxLevel; i++)
        {
            int initializeSize = calculateSize(i);
            int start          = 1 << i;
            int end            = (1 << (i + 1));
            for (int j = start; j < end; j++)
            {
                memoryTree[j] = new MemoryTreeNode(initializeSize, new MemoryArea(j, initializeSize, calcuteOffset(j), memory, this));
            }
        }
        return memoryTree;
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
        if (memoryTree[1].avail < normalizeSize)
        {
            return null;
        }
        freeBytes -= normalizeSize;
        int            hitLevel       = calcuteLevel(normalizeSize);
        int            index          = findNode(normalizeSize, hitLevel);
        MemoryTreeNode memoryTreeNode = memoryTree[index];
        memoryTreeNode.avail = 0;
        updateAllocatedParent(index);
        return ((MemoryTreeNode<T>) memoryTreeNode).memoryArea;
    }

    private int calcuteLevel(int normalizeSize)
    {
        return maxLevel - (MathUtil.log2(normalizeSize) - pageSizeShift);
    }

    @Override
    public void free(int handle)
    {
        int level     = MathUtil.log2(handle);
        int levelSize = calculateSize(level);
        freeBytes += levelSize;
        memoryTree[handle].avail = levelSize;
        int index = handle;
        while (index > 1)
        {
            int parentIndex = index >> 1;
            int value       = memoryTree[index].avail;
            int value2      = memoryTree[index ^ 1].avail;
            levelSize = calculateSize(level);
            if (value == levelSize && value2 == levelSize)
            {
                memoryTree[parentIndex].avail = levelSize << 1;
            }
            else
            {
                memoryTree[parentIndex].avail = value > value2 ? value : value2;
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
            if (memoryTree[childIndex].avail < normalizeSize)
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
            int value       = memoryTree[index].avail;
            int value2      = memoryTree[index ^ 1].avail;
            int parentValue = value > value2 ? value : value2;
            memoryTree[parentIndex].avail = parentValue;
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

    @Override
    public boolean isUnPooled()
    {
        return unpooled;
    }

    @Override
    public int maxLevle()
    {
        return maxLevel;
    }

    @Override
    public T memory()
    {
        return memory;
    }

    @Override
    public int pageSize()
    {
        return pageSize;
    }

    class MemoryTreeNode<T>
    {
        int           avail;
        MemoryArea<T> memoryArea;

        public MemoryTreeNode(int avail, MemoryArea<T> memoryArea)
        {
            this.avail = avail;
            this.memoryArea = memoryArea;
        }
    }
}
