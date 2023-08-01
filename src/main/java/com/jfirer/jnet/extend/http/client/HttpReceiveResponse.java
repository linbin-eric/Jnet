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

    class PartOfBody
    {
        /**
         * 1：代表这是一个完整的，明确总长度的消息体的任意部分。
         * 2：代表这是一个不明确长度，即响应的类型是 Transfer-Encoding：chunked 的消息体的一个完整 Chunk。
         * 3：代表这是一个表达消息体已经结束的特定对象。该对象本身没有消息体数据。
         * 一个 Http 响应的带数据的消息体的类型只会全部是 1 或者 2.
         */
        private int      type;
        private IoBuffer originData;
        /**
         * 在类型 2 的情况下，完整 chunk 的头行的长度（包含头行尾部的/r/n）
         */
        private int      chunkHeaderLength;
        /**
         * 在类型 2 的情况下，完整 chunk 的内容体的长度（不包含代表 chunk 结尾的/r/n）
         */
        private int      chunkSize;

        public IoBuffer getEffectiveContent()
        {
            if (type == 1)
            {
                return originData;
            }
            else if (type == 2)
            {
                originData.addReadPosi(chunkHeaderLength);
                originData.addWritePosi(-2);
                return originData;
            }
            else
            {
                return null;
            }
        }

        public IoBuffer getFullOriginData()
        {
            return originData;
        }
    }

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
                default -> throw new IllegalStateException("Unexpected value: " + state);
            }
        }
    }

    /**
     * 阻塞等待整个响应体的数据读取完毕。
     * 注意：该方法读取后，响应体就没有数据了。默认情况下，使用方法getUTF8Body会在内部调用该方法。
     *
     * @return
     * @throws InterruptedException
     */
    public IoBuffer waitForAllBodyPart() throws InterruptedException
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
                default -> throw new IllegalStateException("Unexpected value: " + state);
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
