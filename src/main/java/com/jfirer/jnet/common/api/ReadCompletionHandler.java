package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.api.ReadCompletionHandler.ReadEntry;
import com.jfirer.jnet.common.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface ReadCompletionHandler<E> extends CompletionHandler<Integer, ReadEntry>
{

    /**
     * 开始监听数据
     */
    void start();

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
