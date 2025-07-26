package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.coder.AbstractDecoder;
import com.jfirer.jnet.common.util.DataIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Slf4j
@Data
public class SSLDecoder extends AbstractDecoder
{
    private final SSLEngine  sslEngine;
    private final SSLEncoder sslEncoder;
    private       boolean    handshakeFinished = false;
    private       int        count             = 1;
    private       boolean    closeInbound      = false;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (handshakeFinished)
        {
            handData(next);
        }
        else
        {
            handshake(next, null);
        }
    }

    private void handshake(ReadProcessorNode next, SSLEngineResult.HandshakeStatus hs)
    {
        while (true)
        {
            if (hs == null)
            {
                hs = sslEngine.getHandshakeStatus();
            }
            if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP || hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN)
            {
                if (accumulation.remainRead() > 0)
                {
                    IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                    try
                    {
                        while (true)
                        {
                            SSLEngineResult        result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                            SSLEngineResult.Status status = result.getStatus();
                            if (status == SSLEngineResult.Status.OK)
                            {
                                accumulation.addReadPosi(result.bytesConsumed());
                                dst.free();
                                hs = result.getHandshakeStatus();
                                log.debug("当前步骤:{},握手阶段unwrap成功，后续阶段为:{}", count++, result.getHandshakeStatus());
                                break;
                            }
                            else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                            {
                                dst.capacityReadyFor(sslEngine.getSession().getApplicationBufferSize());
                                log.debug("当前步骤:{},握手阶段unwrap失败，因为输出空间不足，扩容后继续。", count);
                            }
                            else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                            {
                                dst.free();
                                accumulation.compact();
                                if (accumulation.remainWrite() < 1000)
                                {
                                    accumulation.capacityReadyFor(accumulation.capacity() * 2);
                                }
                                log.debug("当前步骤:{},握手阶段unwrap失败，因为输入空间不足，读取更多数据后继续尝试。", count);
                                return;
                            }
                            else if (status == SSLEngineResult.Status.CLOSED)
                            {
                                dst.free();
                                accumulation.addReadPosi(result.bytesConsumed());
                                if (closeInbound == false)
                                {
                                    closeInbound = true;
                                    sslEngine.closeInbound();
                                    next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
                                }
                                return;
                            }
                        }
                    }
                    catch (SSLException e)
                    {
                        log.debug("当前步骤:{},握手阶段unwrap失败，状态为:{},因为ssl异常，握手失败。", count, hs);
                        dst.free();
                        throw new RuntimeException(e);
                    }
                }
                else
                {
                    accumulation.compact();
                    if (accumulation.remainWrite() < 1000)
                    {
                        accumulation.capacityReadyFor(accumulation.capacity() * 2);
                    }
                    log.debug("当前步骤:{},握手阶段unwrap失败，因为输入空间不足，读取更多数据后继续尝试。", count);
                    return;
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP)
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    while (true)
                    {
                        SSLEngineResult.Status status = result.getStatus();
                        if (status == SSLEngineResult.Status.OK)
                        {
                            dst.addWritePosi(result.bytesProduced());
                            next.pipeline().fireWrite(dst);
                            log.debug("当前步骤:{},握手阶段wrap成功，发送数据。", count++);
                            hs = result.getHandshakeStatus();
                            break;
                        }
                        else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                        {
                            dst.capacityReadyFor(sslEngine.getSession().getApplicationBufferSize());
                            log.debug("当前步骤:{},握手阶段wrap失败，因为输出空间不足，扩容后继续。", count);
                        }
                        else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                        {
                            log.debug("当前步骤:{},握手阶段wrap失败，因为输入空间不足，继续尝试。", count);
                            dst.free();
                            log.error("握手的输出阶段不应该出现这种情况才对");
                            System.exit(3);
                            return;
                        }
                        else if (status == SSLEngineResult.Status.CLOSED)
                        {
                            dst.free();
                            if (closeInbound == false)
                            {
                                closeInbound = true;
                                sslEngine.closeInbound();
                                next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
                            }
                            return;
                        }
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    throw new RuntimeException(e);
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK)
            {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null)
                {
                    task.run();
                }
                hs = null;
            }
            else if (hs == SSLEngineResult.HandshakeStatus.FINISHED || hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            {
                handshakeFinished = true;
                sslEncoder.setSslEngine(sslEngine);
                log.debug("当前步骤:{},握手成功，开始处理数据。", count);
                handData(next);
                return;
            }
        }
    }

    private void handData(ReadProcessorNode next)
    {
        if (accumulation.remainRead() == 0)
        {
            return;
        }
        IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
        try
        {
            while (true)
            {
                SSLEngineResult        result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                SSLEngineResult.Status status = result.getStatus();
                if (status == SSLEngineResult.Status.OK)
                {
                    accumulation.addReadPosi(result.bytesConsumed());
                    dst.addWritePosi(result.bytesProduced());
                    next.fireRead(dst);
                    accumulation.compact();
                    log.debug("当前步骤:{},数据处理阶段unwrap成功，发送数据。", count++);
                    return;
                }
                else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                {
                    dst.capacityReadyFor(sslEngine.getSession().getApplicationBufferSize());
                    log.debug("当前步骤:{},数据处理阶段unwrap失败，因为输出空间不足，扩容后继续。", count++);
                }
                else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                {
                    accumulation.compact();
                    if (accumulation.capacity() < sslEngine.getSession().getPacketBufferSize())
                    {
                        accumulation.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
                    }
                    return;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    accumulation.addReadPosi(result.bytesConsumed());
                    dst.free();
                    if (closeInbound == false)
                    {
                        closeInbound = true;
                        sslEngine.closeInbound();
                        next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
                    }
                    return;
                }
            }
        }
        catch (SSLException e)
        {
            dst.free();
            throw new RuntimeException(e);
        }
    }

    public static class SSLCloseNotify implements DataIgnore
    {
        public static SSLCloseNotify INSTANCE = new SSLCloseNotify();
    }
}
