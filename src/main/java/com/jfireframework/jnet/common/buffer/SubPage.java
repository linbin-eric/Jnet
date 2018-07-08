package com.jfireframework.jnet.common.buffer;

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
        elementSize = 0;
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
        head.next = this;
        next = succeed;
        succeed.prev = this;
        prev = head;
    }
    
    public long allocate()
    {
        if (numAvail == 0)
        {
            return -1;
        }
        int bitmapIdx = allocateFromBitMap();
        if (bitmapIdx == -1)
        {
            return -1;
        }
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] |= 1l << i;
        numAvail--;
        if (numAvail == 0)
        {
            removeFromArena();
        }
        return toHandle(bitmapIdx);
    }
    
    private void removeFromArena()
    {
        next.prev = prev;
        prev.next = next;
        prev = next = null;
    }
    
    private int allocateFromBitMap()
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
                for (int j = 0; j < 64 && memoryIdx < maxNumAvail; j++)
                {
                    if ((bits & 1) == 0)
                    {
                        return memoryIdx;
                    }
                    bits >>>= 1;
                    memoryIdx += 1;
                }
                return -1;
            }
        }
        return -1;
    }
    
    long toHandle(int memoryIdx)
    {
        // 由于bitMapIdx的初始值是0，为了表达这个0是具备含义的，因此在低3位使用一个1来使得整体高32位不会为0
        return 0x4000000000000000L | ((long) memoryIdx << 32) | allocationsCapacityIdx;
    }
    
    /**
     * 返回true意味着该SubPage还在Arena的链表中
     * 
     * @return
     */
    public boolean free(long handle, int allocationsCapacityIdx, int bitmapIdx, SubPage<T> head, Arena<T> arena)
    {
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] &= ~(1 << i);
        numAvail++;
        nextAvail = bitmapIdx;
        if (numAvail == 1)
        {
            addToArena(elementSize, arena);
            return true;
        }
        if (numAvail == maxNumAvail)
        {
            if (next == head && prev == head)
            {
                return true;
            }
            else
            {
                removeFromArena();
                return false;
            }
        }
        return true;
    }
}
