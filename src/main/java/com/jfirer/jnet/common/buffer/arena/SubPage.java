package com.jfirer.jnet.common.buffer.arena;

import com.jfirer.jnet.common.util.ReflectUtil;

public class SubPage
{
    final         int           pageSize;
    final         int           handle;
    final         int           offset;
    final         int           index;
    final         long[]        bitMap;
    private final ChunkListNode node;
    int     elementSize;
    int     bitMapLength;
    int     nextAvail;
    int     maxNumAvail;
    int     numAvail;
    SubPage prev;
    SubPage next;

    public SubPage(ChunkListNode node, int pageSize, int handle, int offset)
    {
        this.node = node;
        this.handle = handle;
        this.offset = offset;
        this.pageSize = pageSize;
        index = handle ^ (1 << node.maxLevel());
        // elementSize最小是16。一个long可以表达64个元素
        bitMap = new long[pageSize >> 4 >> 6];
    }

    public SubPage()
    {
        pageSize = 0;
        handle = 0;
        offset = 0;
        index = 0;
        bitMap = null;
        node = null;
        prev = next = this;
    }

    public void reset(int elementSize)
    {
        this.elementSize = elementSize;
        numAvail = maxNumAvail = pageSize / elementSize;
        nextAvail = 0;
        bitMapLength = (maxNumAvail & 63) == 0 ? maxNumAvail >>> 6 : (maxNumAvail >>> 6) + 1;
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
        return toHandle(bitmapIdx);
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

    long toHandle(int bitmapIdx)
    {
        // 由于bitMapIdx的初始值是0，为了表达这个0是具备含义的，因此在低3位使用一个1来使得整体高32位不会为0
        return 0x4000000000000000L | ((long) bitmapIdx << 32) | (handle);
    }

    public void free(int bitmapIdx)
    {
        nextAvail = bitmapIdx;
        int r = bitmapIdx >>> 6;
        int i = bitmapIdx & 63;
        bitMap[r] ^= 1L << i;
        numAvail++;
    }

    public Chunk chunk()
    {
        return node;
    }

    public int handle()
    {
        return handle;
    }

    public int index()
    {
        return index;
    }

    public boolean empty()
    {
        return numAvail == 0;
    }

    public boolean allAvail()
    {
        return numAvail == maxNumAvail;
    }

    public boolean oneAvail()
    {
        return numAvail == 1;
    }

    public int elementSize()
    {
        return elementSize;
    }

    public int numOfAvail()
    {
        return numAvail;
    }

    public ChunkListNode getChunkListNode()
    {
        return node;
    }

    public SubPage getNext()
    {
        return next;
    }
}
