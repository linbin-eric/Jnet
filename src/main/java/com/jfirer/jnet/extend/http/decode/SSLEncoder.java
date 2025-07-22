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
    private final    SSLEngine sslEngine;
    private volatile boolean   handshakeFinished = false;

    @Override
    public void write(IoBuffer data, WriteProcessorNode next)
    {
        if (handshakeFinished)
        {
            next.fireWrite(data);
        }
        else
        {
            BufferAllocator allocator = next.pipeline().allocator();
            IoBuffer        dst       = allocator.allocate((int) (data.remainRead() * 1.2));
            try
            {
                SSLEngineResult result = sslEngine.wrap(data.readableByteBuffer(), dst.writableByteBuffer());
                switch (result.getStatus())
                {
                    case OK ->
                    {
                        data.free();
                        dst.addWritePosi(result.bytesProduced());
                        next.fireWrite(dst);
                    }
                }
            }
            catch (SSLException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
