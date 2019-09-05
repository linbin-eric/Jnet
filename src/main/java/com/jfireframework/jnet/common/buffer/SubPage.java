package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.ReflectUtil;

public class SubPage<T>
{
    final Chunk<T> chunk;
    final int      pageSize;
    final int      allocationsCapacityIdx;
    final int      offset;
    int    elementSize;
    long[] bitMap;
    int    bitMapLength;
    int    nextAvail;
    int    maxNumAvail;
    int    numAvail;
    SubPage<T> prev;
    SubPage<T> next;

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

    public static void main(String[] args)
    {
        long l      = -1L;
        long result = l & (~(1 << 31));
        System.out.println(result);
        System.out.println(Integer.MAX_VALUE);
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
        SubPage<T> head    = arena.findSubPageHead(elementSize);
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
            ReflectUtil.throwException(new IllegalStateException());
        }
        int bitmapIdx = findAvail();
        if (bitmapIdx == -1)
        {
            ReflectUtil.throwException(new IllegalStateException());
        }
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] |= 1L << i;
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

    private int findAvail()
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
                int bitmapIdx = i << 6;
                for (int j = 0; j < 64 && bitmapIdx < maxNumAvail; j++)
                {
                    if ((bits & 1) == 0)
                    {
                        return bitmapIdx;
                    }
                    bits >>>= 1;
                    bitmapIdx += 1;
                }
                return -1;
            }
        }
        return -1;
    }

    long toHandle(int memoryIdx)
    {
        // 由于bitMapIdx的初始值是0，为了表达这个0是具备含义的，因此在低3位使用一个1来使得整体高32位不会为0
        return 0x4000000000000000L | ((long) memoryIdx << 32) | (allocationsCapacityIdx);
    }

    /**
     * 返回true意味着该SubPage还在Arena的链表中
     *
     * @return
     */
    public boolean free(long handle, int bitmapIdx, SubPage<T> head, Arena<T> arena)
    {
        nextAvail = bitmapIdx;
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] ^= 1L << i;
        numAvail++;
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
