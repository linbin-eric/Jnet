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

@Data
@Slf4j
public class SSLEncoder implements WriteProcessor<IoBuffer>
{
    private volatile SSLEngine sslEngine;

    @Override
    public void write(IoBuffer data, WriteProcessorNode next)
    {
        if (sslEngine == null)
        {
            log.debug("还在握手阶段，直接输出数据");
            next.fireWrite(data);
        }
        else
        {
            BufferAllocator allocator = next.pipeline().allocator();
            IoBuffer        dst       = allocator.allocate((int) (data.remainRead() * 1.2));
            while (true)
            {
                try
                {
                    SSLEngineResult        result = sslEngine.wrap(data.readableByteBuffer(), dst.writableByteBuffer());
                    SSLEngineResult.Status status = result.getStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        data.free();
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
