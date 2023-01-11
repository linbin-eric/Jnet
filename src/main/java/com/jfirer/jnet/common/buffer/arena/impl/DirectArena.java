package com.jfirer.jnet.common.buffer.arena.impl;

import com.jfirer.jnet.common.buffer.Bits;
import com.jfirer.jnet.common.buffer.arena.Chunk;
import com.jfirer.jnet.common.util.PlatFormFunction;
import com.jfirer.jnet.common.util.ReflectUtil;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;

@SuppressWarnings("restriction")
public class DirectArena extends AbstractArena<ByteBuffer>
{
    public DirectArena(int maxLevel, int pageSize, String name)
    {
        super(maxLevel, pageSize, name);
    }

    @Override
    protected ChunkListNode newChunk(int maxLevel, int pageSize, ChunkList chunkList)
    {
        return new DirectChunk(maxLevel, pageSize, chunkList);
    }

    @Override
    protected Chunk<ByteBuffer> newHugeChunk(int reqCapacity)
    {
        return new DirectChunk(reqCapacity);
    }

    @Override
    protected void destoryChunk(Chunk<ByteBuffer> chunk)
    {
        try
        {
            UNSAFE.freeMemory(chunk.memory());
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
    protected void memoryCopy(ByteBuffer src, int srcOffset, ByteBuffer desc, int destOffset, int oldWritePosi)
    {
        long srcAddress  = PlatFormFunction.bytebufferOffsetAddress(src) + srcOffset;
        long destAddress = PlatFormFunction.bytebufferOffsetAddress(desc) + destOffset;
        Bits.copyDirectMemory(srcAddress, destAddress, oldWritePosi);
    }
}
