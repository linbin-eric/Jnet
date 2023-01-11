package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.AbstractBuffer;
import com.jfirer.jnet.common.buffer.PooledBuffer;
import com.jfirer.jnet.common.buffer.arena.Arena;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.buffer.arena.SubPage;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.ReflectUtil;

public abstract class AbstractArena<T> implements Arena<T>
{
    final           int          pageSize;
    final           int          pageSizeShift;
    final           int          maxLevel;
    final           int          subpageOverflowMask;
    //二叉树最左叶子节点的下标值。该值与叶子节点下标相与，可快速得到叶子节点对应的SubPage对象，在SubPage[]中的下标值。
    protected final int          subPageIdxMask;
    final           int          chunkSize;
    final           int          smallestMask = ~15;
    final           ChunkList<T> c000;
    final           ChunkList<T> c025;
    final           ChunkList<T> c050;
    final           ChunkList<T> c075;
    final           ChunkList<T> c100;
    final           ChunkList<T> cInt;
    SubPageListNode[] subPageHeads;
    /**
     * 统计相关
     **/
    int               newChunkCount  = 0;
    int               hugeChunkCount = 0;
    String            name;

    @SuppressWarnings("unchecked")
    public AbstractArena(int maxLevel, int pageSize, final String name)
    {
        if (pageSize < 4096)
        {
            ReflectUtil.throwException(new IllegalArgumentException("pagesize不能小于4096"));
        }
        this.maxLevel = maxLevel;
        this.pageSize = pageSize;
        this.pageSizeShift = MathUtil.log2(pageSize);
        this.subpageOverflowMask = ~(pageSize - 1);
        subPageIdxMask = 1 << maxLevel;
        this.name = name;
        chunkSize = (1 << maxLevel) * pageSize;
        c100 = new ChunkList<>(100, 100, null, chunkSize, this);
        c075 = new ChunkList<>(75, 99, c100, chunkSize, this);
        c050 = new ChunkList<>(50, 90, c075, chunkSize, this);
        c025 = new ChunkList<>(25, 75, c050, chunkSize, this);
        c000 = new ChunkList<>(1, 50, c025, chunkSize, this);
        cInt = new ChunkList<>(0, 25, c000, chunkSize, this);
        c100.setPrevList(c075);
        c075.setPrevList(c050);
        c050.setPrevList(c025);
        c025.setPrevList(c000);
        //从 16 开始，每一次右移都占据一个操作，直到 pagesize 大小
        subPageHeads = new SubPageListNode[pageSizeShift - 4];
        for (int i = 0; i < subPageHeads.length; i++)
        {
            subPageHeads[i] = new SubPageListNode();
        }
    }

    int smallIdx(int normalizeCapacity)
    {
        return MathUtil.log2(normalizeCapacity) - 4;
    }

    boolean isTiny(int normCapacity)
    {
        /**
         * 0~511的数字大小
         */
        return (normCapacity & 0xFFFFFE00) == 0;
    }

    protected abstract Chunk<T> newChunk(int maxLevel, int pageSize);

    protected abstract Chunk<T> newHugeChunk(int reqCapacity);

    private void allocateHuge(int reqCapacity, PooledBuffer<T> buffer)
    {
        Chunk<T> chunk = newHugeChunk(reqCapacity);
        buffer.init(this, null, chunk, reqCapacity, 0, 0);
        hugeChunkCount++;
    }

    protected abstract void destoryChunk(Chunk<T> chunk);

    @Override
    public void allocate(int reqCapacity, PooledBuffer<T> buffer)
    {
        int normalizeCapacity = normalizeCapacity(reqCapacity);
        if (isSmall(normalizeCapacity))
        {
            SubPageListNode head = subPageHeads[smallIdx(normalizeCapacity)];
            synchronized (head)
            {
                SubPageListNode succeed = head.getNext();
                if (succeed != head)
                {
                    long handle = succeed.getSubPage().allocate();
                    initSubPageBuffer(handle, succeed, buffer);
                    if (succeed.getSubPage().empty())
                    {
                        removeFromArena(succeed);
                    }
                    return;
                }
            }
            SubPageListNode subPageProxy = allocateSubPage();
            subPageProxy.getSubPage().reset(normalizeCapacity);
            synchronized (head)
            {
                addToArena(subPageProxy, head);
                long allocate = subPageProxy.getSubPage().allocate();
                initSubPageBuffer(allocate, subPageProxy, buffer);
            }
        }
        else if (normalizeCapacity <= chunkSize)
        {
            allocateNormal(normalizeCapacity, buffer);
        }
        else
        {
            allocateHuge(reqCapacity, buffer);
        }
    }

    private void removeFromArena(SubPageListNode subPageProxy)
    {
        SubPageListNode prev = subPageProxy.getPrev();
        SubPageListNode next = subPageProxy.getNext();
        prev.setNext(next);
        next.setPrev(prev);
        subPageProxy.setPrev(null);
        subPageProxy.setNext(null);
    }

    private void addToArena(SubPageListNode subPageProxy, SubPageListNode head)
    {
        SubPageListNode next = head.getNext();
        head.setNext(subPageProxy);
        subPageProxy.setNext(next);
        next.setPrev(subPageProxy);
        subPageProxy.setPrev(head);
    }

    private int bitmapIdx(long handle)
    {
        return ((int) (handle >>> 32)) & 0x3FFFFFFF;
    }

    private void initSubPageBuffer(long handle, SubPageListNode subPageListNode, PooledBuffer<T> buffer)
    {
        int allocationsCapacityIdx = allocationsCapacityIdx(handle);
        int bitmapIdx              = bitmapIdx(handle);
        int offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPageListNode.getSubPage().elementSize();
        buffer.init(this, subPageListNode.getChunkListNode(), subPageListNode.getSubPage().chunk(), subPageListNode.getSubPage().elementSize(), offset, handle);
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

    int allocationsCapacityIdx(long handle)
    {
        return (int) handle;
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    private synchronized SubPageListNode allocateSubPage()
    {
        SubPageListNode subPageProxy;
        if ((subPageProxy = c050.allocateSubpage()) != null //
            || (subPageProxy = c025.allocateSubpage()) != null//
            || (subPageProxy = c000.allocateSubpage()) != null//
            || (subPageProxy = cInt.allocateSubpage()) != null//
            || (subPageProxy = c075.allocateSubpage()) != null)
        {
            return subPageProxy;
        }
        Chunk<T> chunk = newChunk(maxLevel, pageSize);
        cInt.add(new ChunkListNode<>(cInt, chunk));
        newChunkCount++;
        subPageProxy = cInt.allocateSubpage();
        return subPageProxy;
    }

    private synchronized void allocateNormal(int normalizeCapacity, PooledBuffer<T> buffer)
    {
        if (c050.allocate(normalizeCapacity, buffer)//
            || c025.allocate(normalizeCapacity, buffer)//
            || c000.allocate(normalizeCapacity, buffer)//
            || cInt.allocate(normalizeCapacity, buffer)//
            || c075.allocate(normalizeCapacity, buffer))
        {
            return;
        }
        Chunk<T> chunk = newChunk(maxLevel, pageSize);
        cInt.add(new ChunkListNode<>(cInt, chunk));
        newChunkCount++;
        cInt.allocate(normalizeCapacity, buffer);
    }

    @Override
    public void reAllocate(ChunkListNode<T> oldChunkListNode, PooledBuffer<T> buf, int newReqCapacity)
    {
        AbstractBuffer<T> buffer       = (AbstractBuffer) buf;
        Chunk<T>          oldChunk     = buf.chunk();
        long              oldHandle    = buf.handle();
        int               oldReadPosi  = buffer.getReadPosi();
        int               oldWritePosi = buffer.getWritePosi();
        int               oldCapacity  = buffer.capacity();
        int               oldOffset    = buffer.getOffset();
        T                 oldMemory    = buffer.memory();
        allocate(newReqCapacity, buf);
        if (newReqCapacity > oldCapacity)
        {
            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
            memoryCopy(oldMemory, oldOffset, buffer.memory(), buffer.getOffset(), oldWritePosi);
        }
        // 这种情况是缩小，目前还不支持
        else
        {
            ReflectUtil.throwException(new UnsupportedOperationException());
        }
        free(oldChunkListNode, oldChunk, oldHandle, oldCapacity);
    }

    protected abstract void memoryCopy(T src, int srcOffset, T desc, int destOffset, int oldWritePosi);

    int normalizeCapacity(int reqCapacity)
    {
        if (reqCapacity >= chunkSize)
        {
            return reqCapacity;
        }
        else if ((reqCapacity & smallestMask) == 0)
        {
            return 16;
        }
        return MathUtil.normalizeSize(reqCapacity);
    }

    boolean isSmall(int normCapacity)
    {
        return (normCapacity & subpageOverflowMask) == 0;
    }

    @Override
    public void free(ChunkListNode<T> chunkListNode, Chunk<T> chunk, long handle, int capacity)
    {
        if (chunk.isUnPooled())
        {
            destoryChunk(chunk);
        }
        else
        {
            if (isSmall(capacity))
            {
                SubPageListNode head = subPageHeads[smallIdx(capacity)];
                synchronized (head)
                {
                    SubPageListNode subPageListNode = chunkListNode.find(subPageIdx((int) handle));
                    SubPage         subPage         = subPageListNode.getSubPage();
                    subPage.free(bitmapIdx(handle));
                    if (subPage.oneAvail())
                    {
                        addToArena(subPageListNode, head);
                    }
                    else if (subPage.allAvail())
                    {
                        if (subPageListNode.getNext() == head && subPageListNode.getPrev() == head)
                        {
                            ;
                        }
                        else
                        {
                            removeFromArena(subPageListNode);
                            synchronized (this)
                            {
                                chunkListNode.getParent().free(chunkListNode, (int) handle);
                            }
                        }
                    }
                }
            }
            else
            {
                synchronized (this)
                {
                    chunkListNode.getParent().free(chunkListNode, (int) handle);
                }
            }
        }
    }

    public abstract boolean isDirect();

    public void capacityStat(CapacityStat capacityStat)
    {
        cInt.stat(capacityStat);
        c025.stat(capacityStat);
        c050.stat(capacityStat);
        c075.stat(capacityStat);
        c100.stat(capacityStat);
    }
}
