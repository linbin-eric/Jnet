package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.api.WriteCompletionHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, WriteEntry>, DataProcessor
{
    /**
     * 提供数据写出。
     * 如果当前写完成器已经处于停止状态，则抛出非法状态异常。
     *
     * @param data
     * @throws IllegalStateException
     */
    @Override
    void process(Object data) throws IllegalStateException;


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

        public void clean()
        {
            if (ioBuffer != null)
            {
                ioBuffer.free();
            }
            ioBuffer = null;
            byteBuffer = null;
        }
    }
}
