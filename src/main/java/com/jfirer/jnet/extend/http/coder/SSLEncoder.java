package com.jfirer.jnet.extend.http.coder;

import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Data
@Slf4j
public class SSLEncoder implements WriteProcessor<Object>
{
    private final SSLEngine  sslEngine;
    private final SSLDecoder sslDecoder;
    private       String     remote;

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (data instanceof SSLDecoder.SSLProtocol sslProtocol)
        {
            if (sslProtocol.isCloseNotify())
            {
                int count = 0;
                while (true)
                {
                    if (count++ > 10)
                    {
                        log.error("严重循环错误");
                    }
                    IoBuffer dst = null;
                    try
                    {
                        dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
                        SSLEngineResult result        = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                        int             bytesProduced = result.bytesProduced();
                        dst.addWritePosi(bytesProduced);
                        if (bytesProduced > 0)
                        {
                            next.fireWrite(dst);
                        }
                        else
                        {
                            dst.free();
                        }
                        dst = null;
                        SSLEngineResult.Status status = result.getStatus();
                        if (status == SSLEngineResult.Status.OK)
                        {
                            if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                            {
                                continue;
                            }
                            else
                            {
                                log.warn("当前连接:{},当前步骤:{},发送协议消息,后续阶段为:{}", remote, count++, result.getHandshakeStatus());
                            }
                            return;
                        }
                        else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                        {
                            ;
                        }
                        else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                        {
                            log.error("不会出现这个结果");
                            System.exit(3);
                        }
                        else if (status == SSLEngineResult.Status.CLOSED)
                        {
                            log.debug("当前连接:{}已经结束", remote);
                            return;
                        }
                    }
                    catch (SSLException e)
                    {
                        if (dst != null)
                        {
                            dst.free();
                        }
                        log.error("当前握手出现错误", e);
                        sslDecoder.gracefulClose(next.pipeline());
                    }
                }
            }
            else
            {
                next.fireWrite(sslProtocol.getData());
            }
        }
        else if (data instanceof IoBuffer buffer)
        {
            BufferAllocator allocator = next.pipeline().allocator();
            ByteBuffer      src       = buffer.readableByteBuffer();
            while (true)
            {
                IoBuffer dst = null;
                try
                {
                    dst = allocator.allocate(sslEngine.getSession().getApplicationBufferSize());
                    SSLEngineResult        result        = sslEngine.wrap(src, dst.writableByteBuffer());
                    SSLEngineResult.Status status        = result.getStatus();
                    int                    bytesProduced = result.bytesProduced();
                    dst.addWritePosi(bytesProduced);
                    if (bytesProduced > 0)
                    {
                        next.fireWrite(dst);
                    }
                    else
                    {
                        dst.free();
                    }
                    dst = null;
                    if (status == SSLEngineResult.Status.OK)
                    {
                        if (src.hasRemaining())
                        {
                            continue;
                        }
                        else
                        {
                            buffer.free();
                            return;
                        }
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        log.debug("当前连接:{},生成加密数据失败，因为输出空间不足", remote);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        log.error("不会出现这个结果");
                        System.exit(3);
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
                        log.debug("当前连接:{},发送加密数据的后续是close", remote);
                        sslDecoder.gracefulClose(next.pipeline());
                    }
                }
                catch (SSLException e)
                {
                    if (dst != null)
                    {
                        dst.free();
                    }
                    log.error("当前连接:{}数据加密阶段异常", remote, e);
                    sslDecoder.gracefulClose(next.pipeline());
                    return;
                }
            }
        }
    }
}
