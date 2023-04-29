package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import lombok.Data;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

@Data
public class HttpReceiveResponse implements AutoCloseable
{
    private              int                     httpCode;
    private              Map<String, String>     headers          = new HashMap<>();
    private              int                     contentLength;
    private              String                  contentType;
    private              IoBuffer                body;
    private              BlockingQueue<IoBuffer> stream;
    public static final  IoBuffer                END_OF_STREAM    = new UnPooledBuffer(BufferType.HEAP);
    public static final  IoBuffer                CLOSE_OF_CHANNEL = new UnPooledBuffer(BufferType.HEAP);
    /**
     * 1代表使用中，0代表已关闭
     */
    private volatile     int                     closed           = 1;
    private static final long                    CLOSED_OFFSET    = UNSAFE.getFieldOffset("closed", HttpReceiveResponse.class);

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    @Override
    public void close() throws Exception
    {
        if (closed == 1 && UNSAFE.compareAndSwapInt(this, CLOSED_OFFSET, 1, 0))
        {
            if (body != null)
            {
                body.free();
            }
            clearStream();
        }
    }

    public String getUTF8Body()
    {
        return StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
    }

    public boolean isClosed()
    {
        return closed == 0;
    }

    /**
     * 释放Stream内的Buffer
     */
    public void clearStream()
    {
        if (stream != null)
        {
            IoBuffer buffer;
            while ((buffer = stream.poll()) != null)
            {
                if (buffer != END_OF_STREAM || buffer != CLOSE_OF_CHANNEL)
                {
                    buffer.free();
                }
            }
        }
    }
}
