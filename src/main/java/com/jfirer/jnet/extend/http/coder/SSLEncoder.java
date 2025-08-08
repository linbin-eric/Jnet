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
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;

@Data
@Slf4j
public class SSLEncoder implements WriteProcessor<Object>
{
    private volatile SSLEngine sslEngine;
    private          String    remote;

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (sslEngine == null)
        {
            log.trace("还在握手阶段，直接输出数据");
            next.fireWrite(data);
        }
        else if (data instanceof SSLDecoder.SSLCloseNotify)
        {
            int count =0;
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
                    SSLEngineResult        result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    SSLEngineResult.Status status = result.getStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        dst.addWritePosi(result.bytesProduced());
                        next.fireWrite(dst);
                        if (result.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_WRAP)
                        {
                            continue;
                        }
                        return;
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        dst.free();
                        dst = null;
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        log.error("不会出现这个结果");
                        System.exit(3);
                    }
                    else if(status==SSLEngineResult.Status.CLOSED){
                        dst.free();
                        return;
                    }
                }
                catch (SSLException e)
                {
                    if (dst != null)
                    {
                        dst.free();
                    }
                    log.error("当前握手出现错误,");
                    throw new RuntimeException(e);
                }
            }
        }
        else if (data instanceof IoBuffer buffer)
        {
            BufferAllocator allocator = next.pipeline().allocator();
            IoBuffer        dst       = allocator.allocate((int) (buffer.remainRead() * 1.2));
            ByteBuffer      src       = buffer.readableByteBuffer();
            while (true)
            {
                try
                {
                    SSLEngineResult        result = sslEngine.wrap(src, dst.writableByteBuffer());
                    SSLEngineResult.Status status = result.getStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        dst.addWritePosi(result.bytesProduced());
                        if (src.hasRemaining())
                        {
                            continue;
                        }
                        else
                        {
                            buffer.free();
                            next.fireWrite(dst);
                            return;
                        }
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        SSLSession session = sslEngine.getSession();
                        int        need    = Math.max(session.getApplicationBufferSize(), session.getPacketBufferSize()) - dst.remainWrite();
                        if (need > 0)
                        {
                            dst.capacityReadyFor(dst.capacity() + need);
                        }
                        else
                        {
                            log.error("不应该出现");
                        }
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        log.error("不会出现这个结果");
                        System.exit(3);
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
