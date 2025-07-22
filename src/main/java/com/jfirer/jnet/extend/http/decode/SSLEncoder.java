package com.jfirer.jnet.extend.http.decode;

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
    private volatile SSLEngine sslEngine;

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (sslEngine == null)
        {
            log.debug("还在握手阶段，直接输出数据");
            next.fireWrite(data);
        }
        else if (data instanceof SSLDecoder.SSLCloseNotify)
        {
            sslEngine.closeOutbound();
            IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
            while (true)
            {
                try
                {
                    SSLEngineResult        result = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    SSLEngineResult.Status status = result.getStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        dst.addWritePosi(result.bytesProduced());
                        next.fireWrite(dst);
                        return;
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        dst.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
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
        else if (data instanceof IoBuffer buffer)
        {
            BufferAllocator allocator = next.pipeline().allocator();
            IoBuffer        dst       = allocator.allocate((int) (buffer.remainRead() * 1.2));
            while (true)
            {
                try
                {
                    SSLEngineResult        result = sslEngine.wrap(buffer.readableByteBuffer(), dst.writableByteBuffer());
                    SSLEngineResult.Status status = result.getStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        buffer.free();
                        dst.addWritePosi(result.bytesProduced());
                        next.fireWrite(dst);
                        return;
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        dst.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
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
