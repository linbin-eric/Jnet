package cc.jfire.jnet.common.buffer.arena;

import cc.jfire.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.Bits;
import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.util.CapacityStat;
import cc.jfire.jnet.common.util.MathUtil;
import cc.jfire.jnet.common.util.ReflectUtil;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
    private final Lock lock = new ReentrantLock();
    /**
     * 统计相关
     **/
    int           newChunkCount  = 0;
    AtomicInteger hugeChunkCount = new AtomicInteger();
    AtomicInteger usedAllocate   = new AtomicInteger();
    String        name;

    public Arena(String name, BufferType bufferType)
    {
        this(PooledBufferAllocator.MAXLEVEL, PooledBufferAllocator.PAGESIZE, name, bufferType);
    }

    @SuppressWarnings("unchecked")
    public Arena(int maxLevel, int pageSize, final String name, BufferType bufferType)
    {
        this.bufferType = bufferType;
        if (pageSize < 4096)
        {
            ReflectUtil.throwException(new IllegalArgumentException("pagesize不能小于4096"));
        }
        this.maxLevel            = maxLevel;
        this.pageSize            = pageSize;
        this.pageSizeShift       = MathUtil.log2(pageSize);
        this.subpageOverflowMask = ~(pageSize - 1);
        subPageIdxMask           = 1 << maxLevel;
        this.name                = name;
        chunkSize                = (1 << maxLevel) * pageSize;
        c100                     = new ChunkList(100, 100, null, chunkSize, this);
        c075                     = new ChunkList(75, 99, c100, chunkSize, this);
        c050                     = new ChunkList(50, 90, c075, chunkSize, this);
        c025                     = new ChunkList(25, 75, c050, chunkSize, this);
        c000                     = new ChunkList(1, 50, c025, chunkSize, this);
        cInt                     = new ChunkList(0, 25, c000, chunkSize, this);
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

    private void allocateHuge(int reqCapacity, ArenaAccepter storageSegment)
    {
        storageSegment.init(this, new Chunk(reqCapacity, bufferType), 0, 0, reqCapacity);
        hugeChunkCount.incrementAndGet();
    }

    public void allocate(int reqCapacity, ArenaAccepter storageSegment)
    {
        int normalizeCapacity = normalizeCapacity(reqCapacity);
        if (isSmall(normalizeCapacity))
        {
            SubPage head = subPageHeads[smallIdx(normalizeCapacity)];
            head.getLock().lock();
            try
            {
                SubPage succeed = head.next;
                if (succeed != head)
                {
                    initSubPageBuffer(succeed, storageSegment);
                    if (succeed.empty())
                    {
                        removeFromArena(succeed);
                    }
                    usedAllocate.incrementAndGet();
                    return;
                }
            }
            finally
            {
                head.getLock().unlock();
            }
            SubPage subPage = allocateSubPage(normalizeCapacity);
            head.getLock().lock();
            try
            {
                addToArena(subPage, head);
                initSubPageBuffer(subPage, storageSegment);
            }
            finally
            {
                head.getLock().unlock();
            }
            usedAllocate.incrementAndGet();
        }
        else if (normalizeCapacity <= chunkSize)
        {
            allocateNormal(normalizeCapacity, storageSegment);
            usedAllocate.incrementAndGet();
        }
        else
        {
            allocateHuge(reqCapacity, storageSegment);
        }
    }

    private void removeFromArena(SubPage subPage)
    {
        SubPage prev = subPage.prev;
        SubPage next = subPage.next;
        prev.next    = next;
        next.prev    = prev;
        subPage.prev = null;
        subPage.next = null;
    }

    private void addToArena(SubPage subPage, SubPage head)
    {
        SubPage next = head.next;
        head.next    = subPage;
        subPage.next = next;
        next.prev    = subPage;
        subPage.prev = head;
    }

    private int bitmapIdx(long handle)
    {
        return ((int) (handle >>> 32)) & 0x3FFFFFFF;
    }

    private void initSubPageBuffer(SubPage subPage, ArenaAccepter storageSegment)
    {
        long handle                 = subPage.allocate();
        int  allocationsCapacityIdx = allocationsCapacityIdx(handle);
        int  bitmapIdx              = bitmapIdx(handle);
        int  offset                 = calcuteOffset(allocationsCapacityIdx) + bitmapIdx * subPage.elementSize();
        storageSegment.init(this, subPage.getChunk(), handle, offset, subPage.elementSize());
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

    private SubPage allocateSubPage(int normalizeCapacity)
    {
        lock.lock();
        try
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
            cInt.add(new Chunk(cInt, maxLevel, pageSize, bufferType));
            newChunkCount++;
            subPage = cInt.allocateSubpage(normalizeCapacity);
            return subPage;
        }
        finally
        {
            lock.unlock();
        }
    }

    private void allocateNormal(int normalizeCapacity, ArenaAccepter storageSegment)
    {
        lock.lock();
        try
        {
            if (c050.allocate(normalizeCapacity, storageSegment)//
                || c025.allocate(normalizeCapacity, storageSegment)//
                || c000.allocate(normalizeCapacity, storageSegment)//
                || cInt.allocate(normalizeCapacity, storageSegment)//
                || c075.allocate(normalizeCapacity, storageSegment))
            {
                return;
            }
            cInt.add(new Chunk(cInt, maxLevel, pageSize, bufferType));
            newChunkCount++;
            cInt.allocate(normalizeCapacity, storageSegment);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void memoryCopy(Object src, long srcNativeAddress, int srcOffset, Object desc, long destNativeAddress, int destOffset, int length)
    {
        switch (bufferType)
        {
            case HEAP -> System.arraycopy(src, srcOffset, desc, destOffset, length);
            case DIRECT, MEMORY -> throw new IllegalArgumentException();
            case UNSAFE -> Bits.copyDirectMemory(srcNativeAddress + srcOffset, destNativeAddress + destOffset, length);
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

    public void free(Chunk chunk, long handle, int capacity)
    {
        if (chunk.isUnPooled())
        {
            hugeChunkCount.decrementAndGet();
            chunk.destory();
        }
        else
        {
            usedAllocate.decrementAndGet();
            if (isSmall(capacity))
            {
                SubPage head = subPageHeads[smallIdx(capacity)];
                head.getLock().lock();
                SubPage subPage = chunk.find(subPageIdx((int) handle));
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
                        lock.lock();
                        if (chunk.getParent().free(chunk, (int) handle))
                        {
                            chunk.destory();
                        }
                        lock.unlock();
                    }
                }
                head.getLock().unlock();
            }
            else
            {
                lock.lock();
                if (chunk.getParent().free(chunk, (int) handle))
                {
                    chunk.destory();
                }
                lock.unlock();
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
        capacityStat.setNumOfUnPooledChunk(capacityStat.getNumOfUnPooledChunk() + hugeChunkCount.get());
        capacityStat.setUsedAllocate(capacityStat.getUsedAllocate() + usedAllocate.get());
    }
}
