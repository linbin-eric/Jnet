package cc.jfire.jnet.common.buffer.arena;

import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.util.MathUtil;
import cc.jfire.jnet.common.util.PlatFormFunction;
import cc.jfire.jnet.common.util.UNSAFE;
import lombok.Getter;
import lombok.Setter;

import java.nio.ByteBuffer;

public class Chunk
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
    @Getter
    protected final int              chunkSize;
    protected final Object           memory;
    protected final long             directBufferAddress;
    protected final BufferType       bufferType;
    /**
     * 非池化，意味着这个chunk整块来完整使用，也就是分配超大内存的时候存在的情况。
     * 此时这个chunk不在整个池子里，就不会有chunkList的相关属性和操作。
     */
    protected final boolean          unpooled;
    // Chunk list specific fields
    private final   SubPage[]        subPages;
    private final   int              subPageIdxMask;
    @Getter
    protected       int              freeBytes;
    @Setter
    private         ChunkList        parent;
    private         Chunk            prev;
    @Setter
    @Getter
    private         Chunk            next;

    /* 供ChunkList使用 */
    public Chunk(ChunkList parent, int maxLevel, int pageSize, BufferType bufferType)
    {
        this.pageSize   = pageSize;
        this.maxLevel   = maxLevel;
        this.bufferType = bufferType;
        pageSizeShift   = MathUtil.log2(pageSize);
        freeBytes       = chunkSize = 1 << (maxLevel + pageSizeShift);
        memory          = initializeMemory(chunkSize);
        memoryTree      = initMemoryTree(maxLevel);
        unpooled        = false;
        if (memory instanceof ByteBuffer buffer && buffer.isDirect())
        {
            directBufferAddress = UNSAFE.bytebufferOffsetAddress(buffer);
        }
        else
        {
            directBufferAddress = 0;
        }
        // Chunk list specific fields
        this.parent         = parent;
        subPageIdxMask      = 1 << maxLevel;
        subPageOverflowMask = ~(pageSize - 1);
        subPages            = new SubPage[1 << maxLevel];
    }

    /**
     * 非池化的Chunk，用于chunkSize大于标准大小的Chunk，此时一个Chunk就是完整的内存区域供使用。
     */
    public Chunk(int chunkSize, BufferType bufferType)
    {
        this.bufferType     = bufferType;
        unpooled            = true;
        this.chunkSize      = chunkSize;
        memory              = initializeMemory(chunkSize);
        maxLevel            = 0;
        pageSizeShift       = 0;
        memoryTree          = null;
        subPageOverflowMask = 0;
        pageSize            = 0;
        parent              = null;
        subPageIdxMask      = 0;
        subPages            = null;
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
        /*
         * 1<<hitLevel得到是该层节点数量，同时也是该层第一个节点的下标，为2的次方幂。<br/>
         * 与index进行异或操作就可以去掉最高位的1，也就是得到了index与该值的差。
         */
        return (allocationsCapacityIdx ^ (1 << level)) << capacityShift;
    }

    private int calculateSizeShift(int level)
    {
        return (maxLevel - level + pageSizeShift);
    }

    protected Object initializeMemory(int size)
    {
        switch (bufferType)
        {
            case HEAP ->
            {
                return new byte[size];
            }
            case UNSAFE ->
            {
                return ByteBuffer.allocateDirect(size);
            }
            case DIRECT, MEMORY -> throw new IllegalArgumentException();
            default -> throw new IllegalStateException("Unexpected value: " + bufferType);
        }
    }

    /**
     * 该chunk是否使用堆外内存
     *
     * @return
     */
    public boolean isDirect()
    {
        return bufferType != BufferType.HEAP;
    }

    /**
     * 分配一个规范化后的容量大小的内存空间，返回该内存空间对应的信息。
     *
     * @param normalizeSize
     * @return
     */
    public MemoryArea allocate(int normalizeSize)
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
        return memoryTreeNode.memoryArea;
    }

    private int calcuteLevel(int normalizeSize)
    {
        return maxLevel - (MathUtil.log2(normalizeSize) - pageSizeShift);
    }

    /**
     * 释放handle对应内存空间的占用。
     *
     * @param handle
     */
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
            index                         = parentIndex;
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

    public long directChunkAddress()
    {
        return directBufferAddress;
    }

    public boolean isUnPooled()
    {
        return unpooled;
    }

    public void destory()
    {
        switch (bufferType)
        {
            case HEAP ->
            {
                ;
            }
            case UNSAFE -> UNSAFE.freeMemory((ByteBuffer) memory);
            case DIRECT, MEMORY -> throw new IllegalArgumentException();
            default -> throw new IllegalStateException("Unexpected value: " + bufferType);
        }
    }

    public int maxLevel()
    {
        return maxLevel;
    }

    public Object memory()
    {
        return memory;
    }

    public int pageSize()
    {
        return pageSize;
    }

    // Chunk list methods
    public SubPage allocateSubPage(int normalizeCapacity)
    {
        if (isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        MemoryArea memoryArea = allocate(pageSize);
        int        subPageIdx = subPageIdx(memoryArea.handle());
        SubPage    subPage    = subPages[subPageIdx];
        if (subPage == null)
        {
            subPages[subPageIdx] = subPage = new SubPage(this, pageSize, memoryArea.handle(), memoryArea.offset());
        }
        subPage.reset(normalizeCapacity);
        return subPage;
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    public SubPage find(int subPageIdx)
    {
        if (isUnPooled())
        {
            throw new UnsupportedOperationException();
        }
        return subPages[subPageIdx];
    }

    public ChunkList getParent()
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        return parent;
    }

    public Chunk getPrev()
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        return prev;
    }

    public void setPrev(Chunk prev)
    {
        if (unpooled)
        {
            throw new UnsupportedOperationException();
        }
        this.prev = prev;
    }

    /**
     * 在chunk中的内存区域信息
     *
     * @param handle   该内存区域的下标节点
     * @param capacity 该内存区域的大小
     * @param offset   该内存区域
     */
    record MemoryArea(int handle, int capacity, int offset, Object memory, Chunk chunk)
    {
    }

    class MemoryTreeNode
    {
        int        avail;
        MemoryArea memoryArea;

        public MemoryTreeNode(int avail, MemoryArea memoryArea)
        {
            this.avail      = avail;
            this.memoryArea = memoryArea;
        }
    }
}
