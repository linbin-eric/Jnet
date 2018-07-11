package com.jfireframework.jnet.common.support;

import java.io.IOException;
import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class SingleWriteHandler extends AbstractWriteHandler
{
    
    public SingleWriteHandler(ChannelContext channelContext, BufferAllocator allocator)
    {
        super(channelContext, allocator);
    }
    
    @Override
    public void completed(Integer result, WriteEntry entry)
    {
        ByteBuffer byteBuffer = entry.getByteBuffer();
        if (byteBuffer.hasRemaining())
        {
            socketChannel.write(byteBuffer, entry, this);
            return;
        }
        entry.getIoBuffer().free();
        entry.clear();
        if (queue.isEmpty() == false)
        {
            pollFromQueueAndWrite(entry, byteBuffer);
        }
        for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
        {
            if (queue.isEmpty() == false)
            {
                pollFromQueueAndWrite(entry, byteBuffer);
                return;
            }
        }
        rest();
    }
    
    private void pollFromQueueAndWrite(WriteEntry entry, ByteBuffer byteBuffer)
    {
        IoBuffer buffer = queue.poll();
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.readableByteBuffer());
        socketChannel.write(byteBuffer, entry, this);
    }
    
    @Override
    public void failed(Throwable exc, WriteEntry entry)
    {
        state = TERMINATION;
        entry.getIoBuffer().free();
        entry.clear();
        while (queue.isEmpty() == false)
        {
            queue.poll().free();
        }
        try
        {
            socketChannel.close();
        }
        catch (IOException e)
        {
            ReflectUtil.throwException(e);
        }
    }
    
    @Override
    void writeQueuedBuffer()
    {
        IoBuffer buffer = queue.poll();
        entry.setIoBuffer(buffer);
        entry.setByteBuffer(buffer.readableByteBuffer());
        socketChannel.write(entry.getByteBuffer(), entry, this);
    }
    
    @Override
    public void close()
    {
        // TODO Auto-generated method stub
        
    }
    
}
