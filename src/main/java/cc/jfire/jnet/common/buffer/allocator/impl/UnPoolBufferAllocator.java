package cc.jfire.jnet.common.buffer.allocator.impl;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.BufferType;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import cc.jfire.jnet.common.util.PlatFormFunction;

import java.nio.ByteBuffer;

public class UnPoolBufferAllocator implements BufferAllocator
{
    private final       boolean               preferDirect;
    public static final UnPoolBufferAllocator DEFAULT = new UnPoolBufferAllocator(false);

    public UnPoolBufferAllocator()
    {
        this(true);
    }

    public UnPoolBufferAllocator(boolean preferDirect)
    {
        this.preferDirect = preferDirect;
    }

    @Override
    public IoBuffer allocate(int initializeCapacity)
    {
        UnPooledBuffer buffer = bufferInstance();
        if (preferDirect)
        {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
            buffer.init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), initializeCapacity, 0, 0, initializeCapacity);
        }
        else
        {
            buffer.init(new byte[initializeCapacity], 0, initializeCapacity, 0, 0, initializeCapacity);
        }
        buffer.initRefCnt();
        return buffer;
    }

    @Override
    public void reAllocate(int initializeCapacity, IoBuffer buffer)
    {
        if (preferDirect)
        {
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(initializeCapacity);
            ((UnPooledBuffer) buffer).init(byteBuffer, PlatFormFunction.bytebufferOffsetAddress(byteBuffer), initializeCapacity, 0, 0, initializeCapacity);
        }
        else
        {
            ((UnPooledBuffer) buffer).init(new byte[initializeCapacity], 0, initializeCapacity, 0, 0, initializeCapacity);
        }
    }

    @Override
    public String name()
    {
        return null;
    }

    @Override
    public UnPooledBuffer bufferInstance()
    {
        if (preferDirect)
        {
            return new UnPooledBuffer(BufferType.UNSAFE, this);
        }
        else
        {
            return new UnPooledBuffer(BufferType.HEAP, this);
        }
    }


    @Override
    public void cycleBufferInstance(IoBuffer buffer)
    {
        ;
    }

}
