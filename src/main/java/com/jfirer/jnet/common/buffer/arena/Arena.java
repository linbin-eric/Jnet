package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.PooledBuffer;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.nio.ByteBuffer;

public class Arena
{
    //二叉树最左叶子节点的下标值。该值与叶子节点下标相与，可快速得到叶子节点对应的SubPage对象，在SubPage[]中的下标值。
    protected final int        subPageIdxMask;
    final           int        pageSize;
    final           int        pageSizeShift;
    final           int        maxLevel;
    final           int        subpageOverflowMask;
    final           int        chunkSize;
    final           int        smallestMask = ~15;
    final           ChunkList  c000;
    final           ChunkList  c025;
    final           ChunkList  c050;
    final           ChunkList  c075;
    final           ChunkList  c100;
    final           ChunkList  cInt;
    final           BufferType bufferType;
    final           SubPage[]  subPageHeads;
    /**
     * 统计相关
     **/
    int    newChunkCount  = 0;
    int    hugeChunkCount = 0;
    String name;

    @SuppressWarnings("unchecked")
    public Arena(int maxLevel, int pageSize, final String name, BufferType bufferType)
    {
        this.bufferType = bufferType;
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
        c100 = new ChunkList(100, 100, null, chunkSize, this);
        c075 = new ChunkList(75, 99, c100, chunkSize, this);
        c050 = new ChunkList(50, 90, c075, chunkSize, this);
        c025 = new ChunkList(25, 75, c050, chunkSize, this);
        c000 = new ChunkList(1, 50, c025, chunkSize, this);
        cInt = new ChunkList(0, 25, c000, chunkSize, this);
        c100.setPrevList(c075);
        c075.setPrevList(c050);
        c050.setPrevList(c025);
        c025.setPrevList(c000);
        //从 16 开始，每一次右移都占据一个操作，直到 pagesize 大小
        subPageHeads = new SubPage[pageSizeShift - 4];
        for (int i = 0; i < subPageHeads.length; i++)
        {
            subPageHeads[i] = new SubPage();
        }
    }

    int smallIdx(int normalizeCapacity)
    {
        return MathUtil.log2(normalizeCapacity) - 4;
    }

    private void allocateHuge(int reqCapacity, PooledBuffer buffer)
    {
        buffer.init(this, new ChunkListNode(reqCapacity, bufferType), reqCapacity, 0, 0);
        hugeChunkCount++;
    }

    public void allocate(int reqCapacity, PooledBuffer buffer)
    {
        int normalizeCapacity = normalizeCapacity(reqCapacity);
        if (isSmall(normalizeCapacity))
        {
            SubPage head = subPageHeads[smallIdx(normalizeCapacity)];
            synchronized (head)
            {
                SubPage succeed = head.next;
                if (succeed != head)
                {
                    initSubPageBuffer(succeed, buffer);
                    if (succeed.empty())
                    {
                        removeFromArena(succeed);
                    }
                    return;
                }
            }
            SubPage subPage = allocateSubPage(normalizeCapacity);
            synchronized (head)
            {
                addToArena(subPage, head);
                initSubPageBuffer(subPage, buffer);
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

    private void removeFromArena(SubPage subPage)
    {
        SubPage prev = subPage.prev;
        SubPage next = subPage.next;
        prev.next = next;
        next.prev = prev;
        subPage.prev = null;
        subPage.next = null;
    }

    private void addToArena(SubPage subPage, SubPage head)
    {
        SubPage next = head.next;
        head.next = subPage;
        subPage.next = next;
        next.prev = subPage;
        subPage.prev = head;
    }

    private int bitmapIdx(long handle)
    {
        return ((int) (handle >>> 32)) & 0x3FFFFFFF;
    }

    private void initSubPageBuffer(SubPage subPage, PooledBuffer buffer)
    {
        long handle                 = subPage.allocate();
        int  allocationsCapacityIdx = allocationsCapacityIdx(handle);
        int  bitmapIdx              = bitmapIdx(handle);
        int  offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPage.elementSize();
        buffer.init(this, subPage.getChunkListNode(), subPage.elementSize(), offset, handle);
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

    private int allocationsCapacityIdx(long handle)
    {
        return (int) handle;
    }

    private int subPageIdx(int allocationsCapacityIdx)
    {
        return allocationsCapacityIdx ^ subPageIdxMask;
    }

    private synchronized SubPage allocateSubPage(int normalizeCapacity)
    {
        SubPage subPage;
        if ((subPage = c050.allocateSubpage(normalizeCapacity)) != null //
            || (subPage = c025.allocateSubpage(normalizeCapacity)) != null//
            || (subPage = c000.allocateSubpage(normalizeCapacity)) != null//
            || (subPage = cInt.allocateSubpage(normalizeCapacity)) != null//
            || (subPage = c075.allocateSubpage(normalizeCapacity)) != null)
        {
            return subPage;
        }
        cInt.add(new ChunkListNode(cInt, maxLevel, pageSize, bufferType));
        newChunkCount++;
        subPage = cInt.allocateSubpage(normalizeCapacity);
        return subPage;
    }

    private synchronized void allocateNormal(int normalizeCapacity, PooledBuffer buffer)
    {
        if (c050.allocate(normalizeCapacity, buffer)//
            || c025.allocate(normalizeCapacity, buffer)//
            || c000.allocate(normalizeCapacity, buffer)//
            || cInt.allocate(normalizeCapacity, buffer)//
            || c075.allocate(normalizeCapacity, buffer))
        {
            return;
        }
        cInt.add(new ChunkListNode(cInt, maxLevel, pageSize, bufferType));
        newChunkCount++;
        cInt.allocate(normalizeCapacity, buffer);
    }

    public void reAllocate(ChunkListNode oldChunkListNode, PooledBuffer buf, int newReqCapacity)
    {
        AbstractBuffer buffer       = buf;
        long           oldHandle    = buf.handle();
        int            oldReadPosi  = buffer.getReadPosi();
        int            oldWritePosi = buffer.getWritePosi();
        int            oldCapacity  = buffer.capacity();
        int            oldOffset    = buffer.offset();
        Object         oldMemory    = buffer.memory();
        allocate(newReqCapacity, buf);
        if (newReqCapacity > oldCapacity)
        {
            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
            memoryCopy(oldMemory, oldOffset, buffer.memory(), buffer.offset(), oldWritePosi);
        }
        // 这种情况是缩小，目前还不支持
        else
        {
            ReflectUtil.throwException(new UnsupportedOperationException());
        }
        free(oldChunkListNode, oldHandle, oldCapacity);
    }

    protected void memoryCopy(Object src, int srcOffset, Object desc, int destOffset, int oldWritePosi)
    {
        switch (bufferType)
        {
            case HEAP -> System.arraycopy(src, srcOffset, desc, destOffset, oldWritePosi);
            case DIRECT, MEMORY -> throw new IllegalArgumentException();
            case UNSAFE ->
                    Bits.copyDirectMemory(PlatFormFunction.bytebufferOffsetAddress((ByteBuffer) src) + srcOffset, PlatFormFunction.bytebufferOffsetAddress((ByteBuffer) desc) + destOffset, oldWritePosi);
        }
    }

    private int normalizeCapacity(int reqCapacity)
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

    private boolean isSmall(int normCapacity)
    {
        return (normCapacity & subpageOverflowMask) == 0;
    }

    public void free(ChunkListNode chunkListNode, long handle, int capacity)
    {
        if (chunkListNode.isUnPooled())
        {
            chunkListNode.destory();
        }
        else
        {
            if (isSmall(capacity))
            {
                SubPage head = subPageHeads[smallIdx(capacity)];
                synchronized (head)
                {
                    SubPage subPage = chunkListNode.find(subPageIdx((int) handle));
                    subPage.free(bitmapIdx(handle));
                    if (subPage.oneAvail())
                    {
                        addToArena(subPage, head);
                    }
                    else if (subPage.allAvail())
                    {
                        if (subPage.next == head && subPage.prev == head)
                        {
                            ;
                        }
                        else
                        {
                            removeFromArena(subPage);
                            synchronized (this)
                            {
                                if (chunkListNode.getParent().free(chunkListNode, (int) handle))
                                {
                                    chunkListNode.destory();
                                }
                            }
                        }
                    }
                }
            }
            else
            {
                synchronized (this)
                {
                    if (chunkListNode.getParent().free(chunkListNode, (int) handle))
                    {
                        chunkListNode.destory();
                    }
                }
            }
        }
    }

    public boolean isDirect()
    {
        return bufferType != BufferType.HEAP;
    }

    public void capacityStat(CapacityStat capacityStat)
    {
        cInt.stat(capacityStat);
        c000.stat(capacityStat);
        c025.stat(capacityStat);
        c050.stat(capacityStat);
        c075.stat(capacityStat);
        c100.stat(capacityStat);
        capacityStat.setNumOfUnPooledChunk(capacityStat.getNumOfUnPooledChunk() + hugeChunkCount);
    }
}
