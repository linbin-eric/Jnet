package com.jfireframework.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.WriteHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface WriteHandler extends CompletionHandler<Integer, WriteEntry>
{
    class WriteEntry
    {
        ByteBuffer byteBuffer;
        IoBuffer   ioBuffer;
        
        public ByteBuffer getByteBuffer()
        {
            return byteBuffer;
        }
        
        public void setByteBuffer(ByteBuffer byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
        
        public IoBuffer getIoBuffer()
        {
            return ioBuffer;
        }
        
        public void setIoBuffer(IoBuffer ioBuffer)
        {
            this.ioBuffer = ioBuffer;
        }
        
        public void clear()
        {
            ioBuffer = null;
            byteBuffer = null;
        }
    }
    
    void write(IoBuffer buffer);
    
    void close();
}
