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
import java.util.concurrent.LinkedBlockingDeque;

@Data
public class HttpReceiveResponse implements AutoCloseable
{
    private              int                     httpCode;
    private              Map<String, String>     headers       = new HashMap<>();
    private              int                     contentLength;
    private              String                  contentType;
    private              IoBuffer                body;
    private volatile     BlockingQueue<IoBuffer> stream;
    public static final  IoBuffer                END_OF_STREAM = new UnPooledBuffer(BufferType.HEAP);
    private volatile     int                     closed        = 1;
    private static final long                    CLOSED_OFFSET = UNSAFE.getFieldOffset("closed", HttpReceiveResponse.class);

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
        }
    }

    public String getUTF8Body()
    {
        return StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
    }

    public BlockingQueue<IoBuffer> getStream()
    {
        if (stream == null)
        {
            stream = new LinkedBlockingDeque<>();
        }
        return stream;
    }
}
