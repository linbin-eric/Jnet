package com.jfireframework.jnet.common.api;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import com.jfireframework.jnet.common.api.ReadCompletionHandler.ReadEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface ReadCompletionHandler extends CompletionHandler<Integer, ReadEntry>
{
    
    /**
     * 开始监听数据
     */
    void start();
    
    /**
     * 绑定通道实例
     * 
     * @param channelContext
     */
    void bind(ChannelContext channelContext);
    
    /**
     * 从上次完成方法的中断处开始，继续读取工作。
     */
    void continueRead();
    
    class ReadEntry
    {
        IoBuffer   ioBuffer;
        ByteBuffer byteBuffer;
        
        public IoBuffer getIoBuffer()
        {
            return ioBuffer;
        }
        
        public void setIoBuffer(IoBuffer ioBuffer)
        {
            this.ioBuffer = ioBuffer;
        }
        
        public ByteBuffer getByteBuffer()
        {
            return byteBuffer;
        }
        
        public void setByteBuffer(ByteBuffer byteBuffer)
        {
            this.byteBuffer = byteBuffer;
        }
        
        public void clear()
        {
            ioBuffer = null;
            byteBuffer = null;
        }
    }
    
}
