package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.api.WriteCompletionHandler.WriteEntry;
import com.jfireframework.jnet.common.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, WriteEntry>, WriteProcessor
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
