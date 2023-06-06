package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.BufferType;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.UnPooledBuffer;
import com.jfirer.jnet.common.util.UNSAFE;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class HttpReceiveResponse implements AutoCloseable
{
    private              long                    bodyReadTimeout   = 1000 * 30;
    private              int                     httpCode;
    private              Map<String, String>     headers           = new HashMap<>();
    private              int                     contentLength;
    private              String                  contentType;
    private              BlockingQueue<IoBuffer> chunked           = new LinkedBlockingQueue<>();
    private              String                  utf8Body;
    private              Runnable                onClose;
    private volatile     boolean                 generatedUTF8Body = false;
    private volatile     int                     state             = PARSE_UN_FINISH;
    private static final int                     PARSE_UN_FINISH   = 0;
    private static final int                     PARSE_FINISH      = 1;
    private static final int                     CLOSE             = 2;
    private static final long                    STATE_OFFSET      = UNSAFE.getFieldOffset("state", HttpReceiveResponse.class);
    private static final IoBuffer                END_OF_CHUNKED    = new UnPooledBuffer(BufferType.HEAP)
    {
        @Override
        public void free()
        {
            ;
        }
    };

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    @Override
    public void close()
    {
        while (true)
        {
            switch (state)
            {
                case PARSE_UN_FINISH ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, PARSE_UN_FINISH, CLOSE))
                    {
                        return;
                    }
                    else
                    {
                    }
                }
                case PARSE_FINISH ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, PARSE_FINISH, CLOSE))
                    {
                        chunked.forEach(IoBuffer::free);
                        onClose.run();
                    }
                    else
                    {
                    }
                }
                case CLOSE ->
                {
                    return;
                }
                default ->
                        throw new IllegalStateException("Unexpected value: " + state);
            }
        }
    }

    private IoBuffer waitForAllBodyPart() throws InterruptedException
    {
        long     timeoutInMs = bodyReadTimeout;
        long     t0          = System.currentTimeMillis();
        IoBuffer buffer      = chunked.poll(timeoutInMs, TimeUnit.MILLISECONDS);
        if (buffer == END_OF_CHUNKED)
        {
            return buffer;
        }
        while (true)
        {
            timeoutInMs = bodyReadTimeout - (System.currentTimeMillis() - t0);
            if (timeoutInMs < 0)
            {
                buffer.free();
                throw new InterruptedException("readBodyTimeout is reach");
            }
            IoBuffer poll = null;
            try
            {
                poll = chunked.poll(timeoutInMs, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e)
            {
                buffer.free();
                throw e;
            }
            if (poll != END_OF_CHUNKED)
            {
                buffer.put(poll);
                poll.free();
            }
            else
            {
                return buffer;
            }
        }
    }

    public String getUTF8Body() throws InterruptedException
    {
        if (utf8Body != null)
        {
            return utf8Body;
        }
        if (generatedUTF8Body == false)
        {
            IoBuffer body = waitForAllBodyPart();
            generatedUTF8Body = true;
            if (body == END_OF_CHUNKED)
            {
                return null;
            }
            else
            {
                utf8Body = StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
                body.free();
                return utf8Body;
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

    public void addChunked(IoBuffer buffer)
    {
        chunked.add(buffer);
    }

    public void endChunked()
    {
        chunked.add(END_OF_CHUNKED);
        while (true)
        {
            switch (state)
            {
                case PARSE_UN_FINISH ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, PARSE_UN_FINISH, PARSE_FINISH))
                    {
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case CLOSE ->
                {
                    chunked.forEach(IoBuffer::free);
                    onClose.run();
                    return;
                }
                default ->
                        throw new IllegalStateException("Unexpected value: " + state);
            }
        }
    }

    public IoBuffer pollChunk() throws InterruptedException
    {
        return chunked.poll(bodyReadTimeout, TimeUnit.MILLISECONDS);
    }

    public static boolean isEndOfChunked(IoBuffer buffer)
    {
        return buffer == END_OF_CHUNKED;
    }

    public long getBodyReadTimeout()
    {
        return bodyReadTimeout;
    }

    public void setBodyReadTimeout(long bodyReadTimeout)
    {
        this.bodyReadTimeout = bodyReadTimeout;
    }

    public int getHttpCode()
    {
        return httpCode;
    }

    public void setHttpCode(int httpCode)
    {
        this.httpCode = httpCode;
    }

    public int getContentLength()
    {
        return contentLength;
    }

    public void setContentLength(int contentLength)
    {
        this.contentLength = contentLength;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public Map<String, String> getHeaders()
    {
        return headers;
    }

    public Runnable getOnClose()
    {
        return onClose;
    }

    public void setOnClose(Runnable onClose)
    {
        this.onClose = onClose;
    }
}
