package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.MathUtil;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractArena<T> implements Arena<T>
{
    final           int          pageSize;
    final           int          pageSizeShift;
    final           int          maxLevel;
    final           int          subpageOverflowMask;
    //二叉树最左叶子节点的下标值。该值与叶子节点下标相与，可快速得到叶子节点对应的SubPage对象，在SubPage[]中的下标值。
    protected final int          subPageIdxMask;
    final           int          chunkSize;
    final           ChunkList<T> c000;
    final           ChunkList<T> c025;
    final           ChunkList<T> c050;
    final           ChunkList<T> c075;
    final           ChunkList<T> c100;
    final           ChunkList<T> cInt;
    //    SubPageImpl<T>[] tinySubPages;
//    SubPageImpl<T>[] smallSubPages;
    SubPageListNode[] subPageHeads;
    // 有多少ThreadCache持有了该Arena
    AtomicInteger     numThreadCaches = new AtomicInteger(0);
    /**
     * 统计相关
     **/
    int               newChunkCount   = 0;
    int               hugeChunkCount  = 0;
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
//        // 在tiny区间，以16为大小，每一个16的倍数都占据一个槽位。为了方便定位，实际上数组的0下标是不使用的
//        tinySubPages = new SubPageImpl[512 >>> 4];
//        for (int i = 0; i < tinySubPages.length; i++)
//        {
//            tinySubPages[i] = new SubPageImpl<T>(pageSize);
//        }
//        // 在small，从1<<9开始，每一次右移都占据一个槽位，直到pagesize大小.
//        smallSubPages = new SubPageImpl[pageSizeShift - 9];
//        for (int i = 0; i < smallSubPages.length; i++)
//        {
//            smallSubPages[i] = new SubPageImpl<T>(pageSize);
//        }
        //从 16 开始，每一次右移都占据一个操作，直到 pagesize 大小
        subPageHeads = new SubPageListNode[pageSizeShift - 4];
        for (int i = 0; i < subPageHeads.length; i++)
        {
            subPageHeads[i] = new SubPageListNode();
        }
    }
//    public int tinySubPageNum()
//    {
//        return tinySubPages.length;
//    }
//
//    public int smallSubPageNum()
//    {
//        return smallSubPages.length;
//    }

    static int tinyIdx(int normalizeCapacity)
    {
        /**
         * 该数字除以 16 的商。商刚好是数组的下标。
         */
        return normalizeCapacity >>> 4;
    }

    static int smallIdx(int normalizeCapacity)
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

    abstract ChunkListNode newChunk(int maxLevel, int pageSize, ChunkList chunkList);

    abstract HugeChunk<T> newHugeChunk(int reqCapacity);

    private void allocateHuge(int reqCapacity, PooledBuffer<T> buffer)
    {
        HugeChunk<T> chunk = newHugeChunk(reqCapacity);
        buffer.init(chunk, reqCapacity, 0, 0);
//        hugeChunk.arena = this;
        hugeChunkCount++;
    }

    abstract void destoryChunk(Chunk<T> chunk);

    @Override
    public void allocate(int reqCapacity, PooledBuffer<T> buffer)
    {
        int normalizeCapacity = normalizeCapacity(reqCapacity);
        if (isSmall(normalizeCapacity))
        {
            SubPageListNode head = subPageHeads[smallIdx(normalizeCapacity)];
//            if (isTiny(normalizeCapacity))
//            {
//                if (cache.allocate(buffer, normalizeCapacity, SizeType.TINY, isDirect()))
//                {
//                    return;
//                }
//                head = tinySubPages[tinyIdx(normalizeCapacity)];
//            }
//            else
//            {
//                if (cache.allocate(buffer, normalizeCapacity, SizeType.SMALL, isDirect()))
//                {
//                    return;
//                }
//                head = smallSubPages[smallIdx(normalizeCapacity)];
//            }
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
//            if (cache.allocate(buffer, normalizeCapacity, SizeType.NORMAL, isDirect()))
//            {
//                return;
//            }
            Chunk.MemoryArea memoryArea = allocateNormal(normalizeCapacity, buffer);
            buffer.init(memoryArea.chunk(), memoryArea.capacity(), memoryArea.offset(), memoryArea.handle());
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
//    protected void initBuf(long handle, PooledBuffer<T> buffer)
//    {
//        int bitMapIdx = bitmapIdx(handle);
//        if (bitMapIdx == 0)
//        {
//            initNormalizeSIzeBuffer(handle, buffer, cache);
//        }
//        else
//        {
//            initSubPageBuffer(handle, bitMapIdx & 0x3FFFFFFF, buffer);
//        }
//    }

    private int bitmapIdx(long handle)
    {
        return ((int) (handle >>> 32)) & 0x3FFFFFFF;
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

    private void initSubPageBuffer(long handle, SubPageListNode subPage, PooledBuffer<T> buffer)
    {
        int allocationsCapacityIdx = allocationsCapacityIdx(handle);
        int bitmapIdx              = bitmapIdx(handle);
        int offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPage.getSubPage().elementSize();
        buffer.init(subPage.getChunkListNode(), subPage.getSubPage().elementSize(), offset, handle);
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
        cInt.add(newChunk(maxLevel, pageSize, cInt));
        newChunkCount++;
        subPageProxy = cInt.allocateSubpage();
        return subPageProxy;
    }

    //    private synchronized void allocateNormal(PooledBuffer<T> buffer, int normalizeCapacity, ThreadCache cache)
//    {
//        if (//
//                c050.allocate(normalizeCapacity, buffer, cache)//
//                || c025.allocate(normalizeCapacity, buffer, cache)//
//                || c000.allocate(normalizeCapacity, buffer, cache)//
//                || cInt.allocate(normalizeCapacity, buffer, cache)//
//                || c075.allocate(normalizeCapacity, buffer, cache))
//        {
//            return;
//        }
//        Chunk<T> chunk = newChunk(maxLevel, pageSize);
//        chunk.arena = this;
//        long handle = chunk.allocate(normalizeCapacity);
//        assert handle > 0;
//        chunk.initBuf(handle, buffer, cache);
//        cInt.addFromPrev(chunk, chunk.usage());
//        newChunkCount++;
//    }
    private synchronized Chunk.MemoryArea allocateNormal(int normalizeCapacity, PooledBuffer<T> buffer)
    {
        Chunk.MemoryArea memoryArea;
        if ((memoryArea = c050.allocate(normalizeCapacity)) != null //
            || (memoryArea = c025.allocate(normalizeCapacity)) != null//
            || (memoryArea = c000.allocate(normalizeCapacity)) != null//
            || (memoryArea = cInt.allocate(normalizeCapacity)) != null//
            || (memoryArea = c075.allocate(normalizeCapacity)) != null)
        {
            return memoryArea;
        }
        cInt.add(newChunk(maxLevel, pageSize, cInt));
        newChunkCount++;
        memoryArea = cInt.allocate(normalizeCapacity);
        return memoryArea;
    }

    @Override
    public void reAllocate(PooledBuffer<T> buf, int newReqCapacity)
    {
        AbstractBuffer<T> buffer       = (AbstractBuffer) buf;
        Chunk<T>          oldChunk     = buf.chunk();
        long              oldHandle    = buf.handle();
        int               oldReadPosi  = buffer.readPosi;
        int               oldWritePosi = buffer.writePosi;
        int               oldCapacity  = buffer.capacity;
        int               oldOffset    = buffer.offset;
        T                 oldMemory    = buffer.memory;
        allocate(newReqCapacity, buf);
        if (newReqCapacity > oldCapacity)
        {
            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
            memoryCopy(oldMemory, oldOffset, buffer.memory, buffer.offset, oldWritePosi);
        }
        // 这种情况是缩小，目前还不支持
        else
        {
            ReflectUtil.throwException(new UnsupportedOperationException());
        }
        free(oldChunk, oldHandle, oldCapacity);
    }
//    public void reAllocate(PooledBuffer<T> buf, int newReqCapacity)
//    {
//        AbstractBuffer<T> buffer       = (AbstractBuffer) buf;
//        ChunkImpl<T>      oldChunk     = buf.getPoolInfoHolder().chunk;
//        long              oldHandle    = buf.getPoolInfoHolder().handle;
//        int               oldReadPosi  = buffer.readPosi;
//        int               oldWritePosi = buffer.writePosi;
//        int               oldCapacity  = buffer.capacity;
//        int               oldOffset    = buffer.offset;
//        T                 oldMemory    = buffer.memory;
//        ThreadCache       oldCache     = buf.getPoolInfoHolder().cache;
//        allocate(newReqCapacity, Integer.MAX_VALUE, buf, parent.threadCache());
//        if (newReqCapacity > oldCapacity)
//        {
//            buffer.setReadPosi(oldReadPosi).setWritePosi(oldWritePosi);
//            memoryCopy(oldMemory, oldOffset, buffer.memory, buffer.offset, oldWritePosi);
//        }
//        // 这种情况是缩小，目前还不支持
//        else
//        {
//            ReflectUtil.throwException(new UnsupportedOperationException());
//        }
//        free(oldChunk, oldHandle, oldCapacity, oldCache);
//    }

    abstract void memoryCopy(T src, int srcOffset, T desc, int destOffset, int oldWritePosi);

    int normalizeCapacity(int reqCapacity)
    {
        if (reqCapacity >= chunkSize)
        {
            return reqCapacity;
        }
        else if (reqCapacity < 16)
        {
            return 16;
        }
//        if (isTiny(reqCapacity))
//        {
//            //获得大于reqCapacity的最小的16的倍数
//            return (reqCapacity & 15) == 0 ? reqCapacity : (reqCapacity & ~15) + 16;
//        }
        return MathUtil.normalizeSize(reqCapacity);
    }

    boolean isSmall(int normCapacity)
    {
        return (normCapacity & subpageOverflowMask) == 0;
    }

    @Override
    public void free(Chunk<T> chunk, long handle, int capacity)
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
                    SubPageListNode subPageListNode = ((ChunkListNode<T>) chunk).find(subPageIdx((int) handle));
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
                                ((ChunkListNode<T>) chunk).getParent().free((ChunkListNode<T>) chunk, (int) handle);
                            }
                        }
                    }
                }
            }
            else
            {
                synchronized (this)
                {
                    ((ChunkListNode<T>) chunk).getParent().free((ChunkListNode<T>) chunk, (int) handle);
                }
            }
        }
    }
//    public void free(ChunkImpl<T> chunk, long handle, int normalizeCapacity, ThreadCache cache)
//    {
//        if (chunk.unpooled == true)
//        {
//            destoryChunk(chunk);
//        }
//        else
//        {
//            if (cache.add(normalizeCapacity, sizeType(normalizeCapacity), isDirect(), chunk, handle))
//            {
//                return;
//            }
//            final boolean destoryChunk;
//            synchronized (this)
//            {
//                destoryChunk = chunk.parent.free(chunk, handle);
//            }
//            if (destoryChunk)
//            {
//                destoryChunk(chunk);
//            }
//        }
//    }

    SizeType sizeType(int normalizeCapacity)
    {
        if (isSmall(normalizeCapacity))
        {
            if (isTiny(normalizeCapacity))
            {
                return SizeType.TINY;
            }
            else
            {
                return SizeType.SMALL;
            }
        }
        return SizeType.NORMAL;
    }
//    SubPageImpl<T> findSubPageHead(int elementSize)
//    {
//        if (isTiny(elementSize))
//        {
//            int tinyIdx = tinyIdx(elementSize);
//            return tinySubPages[tinyIdx];
//        }
//        else
//        {
//            int smallIdx = smallIdx(elementSize);
//            return smallSubPages[smallIdx];
//        }
//    }

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
