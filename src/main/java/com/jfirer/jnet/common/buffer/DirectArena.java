package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.impl.ChunkImpl;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.ReflectUtil;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;

@SuppressWarnings("restriction")
public class DirectArena extends AbstractArena<ByteBuffer>
{
    public DirectArena(PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask, String name)
    {
        super(parent, maxLevel, pageSize, pageSizeShift, subpageOverflowMask, name);
    }

    @Override
    ChunkImpl<ByteBuffer> newChunk(int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
    {
        return new DirectChunk(maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
    }

    @Override
    ChunkImpl<ByteBuffer> newChunk(int reqCapacity, AbstractArena<ByteBuffer> tAbstractArena)
    {
        return new DirectChunk(reqCapacity);
    }

    @Override
    void destoryChunk(ChunkImpl<ByteBuffer> chunk)
    {
        try
        {
            UNSAFE.freeMemory(chunk.memory);
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
        }
    }

    @Override
    public boolean isDirect()
    {
        return true;
    }

    @Override
    void memoryCopy(ByteBuffer src, int srcOffset, ByteBuffer desc, int destOffset, int oldWritePosi)
    {
        long srcAddress  = PlatFormFunction.bytebufferOffsetAddress(src) + srcOffset;
        long destAddress = PlatFormFunction.bytebufferOffsetAddress(desc) + destOffset;
        Bits.copyDirectMemory(srcAddress, destAddress, oldWritePosi);
    }
}
