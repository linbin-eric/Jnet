package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.api.WriteCompletionHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, WriteEntry>, DataProcessor
{
    /**
     * 提供数据写出。如果数据放入待发送队列，则返回true。如果队列已满，则返回false<br/>
     * 如果当前写完成器已经处于停止状态，则抛出非法状态异常。
     *
     * @param data
     * @throws IllegalStateException
     */
    @Override
    boolean process(Object data) throws IllegalStateException;

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
}
