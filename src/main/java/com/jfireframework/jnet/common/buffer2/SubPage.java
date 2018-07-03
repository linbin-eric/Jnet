package com.jfireframework.jnet.common.buffer2;

public class SubPage<T>
{
    int            elementSize;
    long[]         bitMap;
    int            bitMapLength;
    int            nextAvail;
    int            maxNumAvail;
    int            numAvail;
    final Chunk<T> chunk;
    final int      pageSize;
    final int      allocationsCapacityIdx;
    final int      offset;
    SubPage<T>     prev;
    SubPage<T>     next;
    
    /**
     * 这是一个特殊节点，不参与分配，仅用做标识
     * 
     * @param pageSize
     */
    public SubPage(int pageSize)
    {
        chunk = null;
        allocationsCapacityIdx = 0;
        offset = 0;
        this.pageSize = pageSize;
        prev = next = this;
    }
    
    public SubPage(Chunk<T> chunk, int pageSize, int allocationsCapacityIdx, int offset, int elementSize, Arena<T> arena)
    {
        this.chunk = chunk;
        this.allocationsCapacityIdx = allocationsCapacityIdx;
        this.offset = offset;
        this.pageSize = pageSize;
        // elementSize最小是16。一个long可以表达64个元素
        bitMap = new long[pageSize >> 4 >> 6];
        init(elementSize, arena);
    }
    
    public void init(int elementSize, Arena<T> arena)
    {
        this.elementSize = elementSize;
        numAvail = maxNumAvail = pageSize / elementSize;
        nextAvail = 0;
        bitMapLength = (maxNumAvail & 63) == 0 ? maxNumAvail >>> 6 : (maxNumAvail >>> 6) + 1;
        addToArena(elementSize, arena);
    }
    
    private void addToArena(int elementSize, Arena<T> arena)
    {
        SubPage<T> head = arena.findSubPageHead(elementSize);
        SubPage<T> succeed = head.next;
        if (succeed == null)
        {
            next = null;
            prev = head;
            head.next = this;
        }
        else
        {
            head.next = this;
            next = succeed;
            succeed.prev = this;
            prev = head;
        }
    }
    
    public long allocate()
    {
        if (numAvail == 0)
        {
            return -1;
        }
        int bitmapIdx = allocateBitMap();
        if (bitmapIdx == -1)
        {
            return -1;
        }
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] |= 1 << i;
        numAvail--;
        if (numAvail == 0)
        {
            removeFromArena();
        }
        return toHandle(bitmapIdx);
    }

    private void removeFromArena()
    {
        if (next == null)
        {
            prev.next = null;
        }
        else
        {
            next.prev = prev;
            prev.next = next;
        }
        next = prev = null;
    }
    
    private int allocateBitMap()
    {
        int nextAvail = this.nextAvail;
        if (nextAvail != -1)
        {
            this.nextAvail = -1;
            return nextAvail;
        }
        for (int i = 0; i < bitMapLength; i++)
        {
            long bits = bitMap[i];
            if (~bits != 0)
            {
                int memoryIdx = i << 6;
                while (memoryIdx < maxNumAvail)
                {
                    if ((bits & 1) == 0)
                    {
                        return memoryIdx;
                    }
                    bits >>= 1;
                    memoryIdx += 1;
                }
                return -1;
            }
        }
        return -1;
    }
    
    long toHandle(int memoryIdx)
    {
        return ((long) memoryIdx << 32) | allocationsCapacityIdx;
    }
    
    /**
     * 返回true意味着该SubPage还在Arena的链表中
     * 
     * @return
     */
    public boolean free(long handle, int allocationsCapacityIdx, int bitmapIdx)
    {
        return false;
    }
}
