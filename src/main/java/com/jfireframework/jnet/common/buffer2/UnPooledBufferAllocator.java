package com.jfireframework.jnet.common.buffer2;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class UnPooledBufferAllocator
{
    
    public static IoBuffer allocate(int capacity)
    {
        UnPooledBuffer<byte[]> buffer = new UnPooledHeapBuffer();
        buffer.init(new byte[capacity], capacity);
        return buffer;
    }
    
    public static IoBuffer allocateDirect(int capacity)
    {
        UnPooledBuffer<ByteBuffer> buffer = new UnPooledDirectBuffer();
        buffer.init(ByteBuffer.allocateDirect(capacity), capacity);
        return buffer;
    }
    
}
