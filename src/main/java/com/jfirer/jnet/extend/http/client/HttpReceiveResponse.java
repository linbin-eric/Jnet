package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.UNSAFE;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;

/**
 * 非线程并发安全的类。使用消息体的方法均只能由一个线程来使用。
 */
@Data
public class HttpReceiveResponse implements AutoCloseable
{
    private static final long                      bodyReadTimeout                 = 1000 * 30;
    private static final int                       RECEIVE_UN_FINISH_AND_NOT_CLOSE = 0b00;
    private static final int                       RECEIVE_UN_FINISH_AND_CLOSE     = 0b01;
    private static final int                       RECEIVE_FINISH_AND_NOT_CLOSE    = 0b10;
    private static final int                       RECEIVE_FINISH_AND_CLOSE        = 0b11;
    private static final long                      STATE_OFFSET                    = UNSAFE.getFieldOffset("state", HttpReceiveResponse.class);
    private final        HttpConnection            connection;
    private              int                       httpCode;
    private              Map<String, String>       headers                         = new HashMap<>();
    /**
     * 值的取值范围有：
     * -1：代表这个响应的消息体是个不定长的以Transfer-Encoding:chunked 编码的消息体。
     * 0：代表没有响应体。
     * 正数：代表响应的消息体的字节长度。
     */
    private              int                       contentLength;
    private              String                    contentType;
    private              BlockingQueue<PartOfBody> body                            = new LinkedBlockingQueue<>();
    @Getter(AccessLevel.NONE)
    private              String                    decodedUTF8Body;
    private volatile     boolean                   generatedUTF8Body               = false;
    private volatile     Thread                    waitForReceiveFinshThread;
    private volatile     int                       state                           = RECEIVE_UN_FINISH_AND_NOT_CLOSE;

    public static boolean isEndOrTerminateOfBody(PartOfBody partOfBody)
    {
        return partOfBody == PartOfBody.END_OF_BODY || partOfBody == PartOfBody.TERMINATE_OF_BODY;
    }

    public static boolean isEndOfBody(PartOfBody partOfBody)
    {
        return partOfBody == PartOfBody.END_OF_BODY;
    }

    public static boolean isTerminateOfBody(PartOfBody partOfBody)
    {
        return partOfBody == PartOfBody.TERMINATE_OF_BODY;
    }

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public void addPartOfBody(PartOfBody partOfBody)
    {
        body.add(partOfBody);
    }

    public void endOfBody()
    {
        body.add(PartOfBody.END_OF_BODY);
        while (true)
        {
            switch (state)
            {
                case RECEIVE_UN_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_NOT_CLOSE, RECEIVE_FINISH_AND_NOT_CLOSE))
                    {
                        LockSupport.unpark(waitForReceiveFinshThread);
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case RECEIVE_UN_FINISH_AND_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_CLOSE, RECEIVE_FINISH_AND_CLOSE))
                    {
                        PartOfBody needClose;
                        while ((needClose = body.poll()) != null)
                        {
                            needClose.freeBuffer();
                        }
                        connection.recycle();
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                default -> throw new IllegalStateException("endOfBody 方法只能由解码器调用，在调用这个方法的时候，消息体流的状态应该是为未解析完成");
            }
        }
    }

    /**
     * 这是因为通道关闭而终止响应。与客户端主动调用的 close 是不同的。
     */
    public void terminate()
    {
        body.add(PartOfBody.TERMINATE_OF_BODY);
        while (true)
        {
            switch (state)
            {
                case RECEIVE_UN_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_NOT_CLOSE, RECEIVE_FINISH_AND_NOT_CLOSE))
                    {
                        PartOfBody needClose;
                        while ((needClose = body.poll()) != null)
                        {
                            needClose.freeBuffer();
                        }
                        body.add(PartOfBody.TERMINATE_OF_BODY);
                        LockSupport.unpark(waitForReceiveFinshThread);
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case RECEIVE_UN_FINISH_AND_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_CLOSE, RECEIVE_FINISH_AND_CLOSE))
                    {
                        PartOfBody needClose;
                        while ((needClose = body.poll()) != null)
                        {
                            needClose.freeBuffer();
                        }
                        body.add(PartOfBody.TERMINATE_OF_BODY);
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                default -> throw new IllegalStateException("terminate 方法调用的时候，流应该是没有解析完全的状态");
            }
        }
    }

    /**
     * 客户端代码消费完毕响应后关闭该响应。该方法应该只能由客户端代码来调用。
     */
    @Override
    public void close()
    {
        while (true)
        {
            switch (state)
            {
                case RECEIVE_UN_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_UN_FINISH_AND_NOT_CLOSE, RECEIVE_UN_FINISH_AND_CLOSE))
                    {
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                case RECEIVE_FINISH_AND_NOT_CLOSE ->
                {
                    if (UNSAFE.compareAndSwapInt(this, STATE_OFFSET, RECEIVE_FINISH_AND_NOT_CLOSE, RECEIVE_FINISH_AND_CLOSE))
                    {
                        PartOfBody needClose;
                        while ((needClose = body.poll()) != null)
                        {
                            needClose.freeBuffer();
                        }
                        connection.recycle();
                        return;
                    }
                    else
                    {
                        ;
                    }
                }
                default -> throw new IllegalStateException("close 方法只能由客户端消费响应后调用，因此状态应该为 not_close 状态");
            }
        }
    }

    /**
     * 超时等待消息体被接收完整。如果超时时间到达仍未接收完整，则抛出异常。
     *
     * @throws TimeoutException
     */
    public void waitForReceiveFinish() throws TimeoutException
    {
        if (state == RECEIVE_UN_FINISH_AND_NOT_CLOSE)
        {
            long timeoutInMs = bodyReadTimeout;
            long t0          = System.currentTimeMillis();
            waitForReceiveFinshThread = Thread.currentThread();
            while (state == RECEIVE_UN_FINISH_AND_NOT_CLOSE)
            {
                LockSupport.park(timeoutInMs);
                timeoutInMs = bodyReadTimeout - (System.currentTimeMillis() - t0);
                if (timeoutInMs < 0)
                {
                    throw new TimeoutException("wait for receive finish timeout!");
                }
            }
        }
        else if (state == RECEIVE_FINISH_AND_NOT_CLOSE)
        {
            ;
        }
        else
        {
            throw new IllegalStateException("作为消费线程，不应该在关闭响应后仍然调用该方法");
        }
    }

    public String getCachedUTF8Body() throws TimeoutException
    {
        if (!generatedUTF8Body)
        {
            waitForReceiveFinish();
            generatedUTF8Body = true;
            IoBuffer   ioBuffer = HttpClient.ALLOCATOR.allocate(512);
            PartOfBody needClose;
            while ((needClose = body.poll()) != null && !needClose.isEndOrTerminateOfBody())
            {
                ioBuffer.put(needClose.getEffectiveContent());
                needClose.freeBuffer();
            }
            decodedUTF8Body = StandardCharsets.UTF_8.decode(ioBuffer.readableByteBuffer()).toString();
            ioBuffer.free();
        }
        return decodedUTF8Body;
    }

    /**
     * 基于超时时间进行的消息体数据提取。如果在超时时间到达前读取到数据则返回，否则返回 null。
     *
     * @return
     * @throws InterruptedException
     */
    public PartOfBody pollChunk() throws InterruptedException
    {
        return body.poll(bodyReadTimeout, TimeUnit.MILLISECONDS);
    }
}
