package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.decoder.AbstractDecoder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Slf4j
@Data
public class SSLRequestDecoder extends AbstractDecoder
{
    private final SSLEngine sslEngine;
    private       boolean   handshakeFinished = false;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (handshakeFinished)
        {
            handData(next);
        }
        else
        {
            handshake(next);
        }
    }

    private void handshake(ReadProcessorNode next)
    {
        switch (sslEngine.getHandshakeStatus())
        {
            case NEED_UNWRAP, NEED_UNWRAP_AGAIN ->
            {
                log.debug("当前握手状态:{}", sslEngine.getHandshakeStatus());
                ByteBuffer byteBuffer = accumulation.readableByteBuffer();
                IoBuffer   dst        = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.unwrap(byteBuffer, dst.writableByteBuffer());
                    switch (result.getStatus())
                    {
                        case OK, BUFFER_OVERFLOW ->
                        {
                            log.debug("当前unwrap结果:{}", result.getStatus());
                            dst.free();
                            accumulation.addReadPosi(result.bytesConsumed());
                            accumulation.compact();
                            handshake(next);
                        }
                        case BUFFER_UNDERFLOW ->
                        {
                            dst.free();
                            accumulation.compact();
                        }
                        case CLOSED ->
                        {
                            dst.free();
                            next.pipeline().shutdownInput();
                        }
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    throw new RuntimeException(e);
                }
            }
            case NEED_WRAP ->
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    switch (result.getStatus())
                    {
                        case OK ->
                        {
                            dst.addWritePosi(result.bytesProduced());
                            next.pipeline().fireWrite(dst);
                            handshake(next);
                        }
                        case BUFFER_OVERFLOW ->
                        {
                            dst.free();
                            handshake(next);
                        }
                        case CLOSED ->
                        {
                            dst.free();
                            next.pipeline().shutdownInput();
                        }
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    throw new RuntimeException(e);
                }
            }
            case NEED_TASK ->
            {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null)
                {
                    task.run();
                }
                handshake(next);
            }
            case FINISHED, NOT_HANDSHAKING ->
            {
                handshakeFinished = true;
                handData(next);
            }
        }
    }

    private void handData(ReadProcessorNode next)
    {
        switch (sslEngine.getHandshakeStatus())
        {
            case FINISHED, NOT_HANDSHAKING, NEED_UNWRAP, NEED_UNWRAP_AGAIN ->
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                    switch (result.getStatus())
                    {
                        case OK ->
                        {
                            accumulation.addReadPosi(result.bytesConsumed());
                            dst.addWritePosi(result.bytesProduced());
                            next.fireRead(dst);
                            handData(next);
                        }
                        case BUFFER_OVERFLOW ->
                        {
                            dst.free();
                            handData(next);
                        }
                        case BUFFER_UNDERFLOW ->
                        {
                            dst.free();
                            accumulation.compact();
                        }
                        case CLOSED ->
                        {
                            dst.free();
                            next.pipeline().shutdownInput();
                        }
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    throw new RuntimeException(e);
                }
            }
            case NEED_TASK ->
            {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null)
                {
                    task.run();
                }
                handData(next);
            }
            case NEED_WRAP ->
            {
                accumulation.compact();
                log.debug("业务数据处理完毕");
            }
        }
    }
}
