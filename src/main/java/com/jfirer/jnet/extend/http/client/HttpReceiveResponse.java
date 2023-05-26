package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.util.ReflectUtil;
import com.jfirer.jnet.common.util.UNSAFE;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Data
public class HttpReceiveResponse implements AutoCloseable
{
    private              int                           httpCode;
    private              Map<String, String>           headers        = new HashMap<>();
    private              int                           contentLength;
    private              String                        contentType;
    private              BlockingQueue<IoBuffer>       chunked        = new LinkedBlockingQueue<>();
    private              String                        utf8Body;
    private              Consumer<HttpReceiveResponse> onClose;
    /**
     * 1代表使用中，0代表已关闭
     */
    private volatile     int                           closed         = 1;
    private volatile     boolean                       consumered     = false;
    @Setter(AccessLevel.NONE)
    @Getter(AccessLevel.NONE)
    private              IoBuffer                      aggregation;
    private static final long                          CLOSED_OFFSET  = UNSAFE.getFieldOffset("closed", HttpReceiveResponse.class);
    private static final IoBuffer                      END_OF_CHUNKED = new UnPooledBuffer(BufferType.HEAP);

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    @Override
    public void close() throws Exception
    {
        if (closed == 1 && UNSAFE.compareAndSwapInt(this, CLOSED_OFFSET, 1, 0))
        {
            if (consumered == false)
            {
                IoBuffer buffer = waitForAllBodyPart();
                if (buffer != null)
                {
                    buffer.free();
                }
            }
            if (onClose != null)
            {
                onClose.accept(this);
            }
        }
    }

    public IoBuffer waitForAllBodyPart()
    {
        if (consumered)
        {
            throw new IllegalStateException("不能重复消费，当前已经消费过该响应内容体");
        }
        consumered = true;
        try
        {
            aggregation = chunked.take();
            if (aggregation != END_OF_CHUNKED)
            {
                do
                {
                    IoBuffer another = chunked.take();
                    if (another != END_OF_CHUNKED)
                    {
                        aggregation.put(another);
                        another.free();
                    }
                    else
                    {
                        break;
                    }
                }
                while (true);
                return aggregation;
            }
            else
            {
                return null;
            }
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
            return null;
        }
    }

    public String getUTF8Body()
    {
        if (utf8Body != null)
        {
            return utf8Body;
        }
        if (consumered == false)
        {
            IoBuffer body = waitForAllBodyPart();
            if (body != null)
            {
                utf8Body = StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
                return utf8Body;
            }
            else
            {
                return null;
            }
        }
        else
        {
            return null;
        }
    }

    public boolean isSuccessful()
    {
        return httpCode == 200;
    }

    /**
     * 释放Stream内的Buffer
     */
    public void clearChunked()
    {
        if (chunked != null)
        {
            IoBuffer buffer;
            while ((buffer = chunked.poll()) != null)
            {
                if (buffer != END_OF_CHUNKED)
                {
                    buffer.free();
                }
            }
        }
    }

    public void addChunked(IoBuffer buffer)
    {
        chunked.add(buffer);
    }

    public void endChunked()
    {
        chunked.add(END_OF_CHUNKED);
    }

    public IoBuffer poll()
    {
        return chunked.poll();
    }

    public IoBuffer poll(long timeout, TimeUnit unit)
    {
        try
        {
            return chunked.poll(timeout, unit);
        }
        catch (InterruptedException e)
        {
            ReflectUtil.throwException(e);
            return null;
        }
    }

    public IoBuffer take() throws InterruptedException
    {
        return chunked.take();
    }

    public static boolean isEndOfChunked(IoBuffer buffer)
    {
        return buffer == END_OF_CHUNKED;
    }
}
