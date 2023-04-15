package com.jfirer.jnet.common.buffer.buffer.impl;

import com.jfirer.jnet.common.buffer.buffer.Bits;
import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.ByteBuffer;

public class UnPooledBuffer extends AbstractBuffer
{
    public UnPooledBuffer(BufferType bufferType)
    {
        super(bufferType);
    }

    @Override
    protected void reAllocate(int posi)
    {
        posi = posi > capacity * 2 ? posi : 2 * capacity;
        switch (bufferType)
        {
            case HEAP ->
            {
                byte[] oldMemory = (byte[]) memory;
                memory = new byte[posi];
                System.arraycopy(oldMemory, 0, memory, 0, writePosi);
                capacity = posi;
            }
            case DIRECT ->
            {
                ByteBuffer src          = (ByteBuffer) memory;
                int        oldReadPosi  = readPosi;
                int        oldWritePosi = writePosi;
                memory = ByteBuffer.allocateDirect(posi);
                init(memory, posi, 0, UNSAFE.bytebufferOffsetAddress((ByteBuffer) memory));
                readPosi = oldReadPosi;
                writePosi = oldWritePosi;
                ((ByteBuffer) memory).put(0, src, 0, writePosi);
            }
            case UNSAFE ->
            {
                long oldAddress   = nativeAddress;
                int  oldReadPosi  = readPosi;
                int  oldWritePosi = writePosi;
                memory = ByteBuffer.allocateDirect(posi);
                init(memory, posi, 0, UNSAFE.bytebufferOffsetAddress((ByteBuffer) memory));
                readPosi = oldReadPosi;
                writePosi = oldWritePosi;
                Bits.copyDirectMemory(oldAddress, nativeAddress, oldWritePosi);
            }
            case MEMORY ->
            {
            }
        }
    }

    @Override
    protected void free0(int capacity)
    {
    }
}
