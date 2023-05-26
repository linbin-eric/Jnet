package com.jfirer.jnet.common.decoder;

import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDecoder implements ReadProcessor<IoBuffer>
{
    protected BufferAllocator allocator;
    protected IoBuffer        accumulation;

    public AbstractDecoder(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    public void read(IoBuffer data, ReadProcessorNode next)
    {
        try
        {
            if (accumulation == null)
            {
                accumulation = data;
            }
            else
            {
                accumulation.put(data);
                data.free();
            }
            process0(next);
        }
        catch (Throwable e)
        {
            System.err.println(this.getClass());
            log.error("解码过程中发生未知异常", e);
            next.pipeline().channelContext().close(e);
        }
    }

    protected abstract void process0(ReadProcessorNode next);

    protected void compactIfNeed()
    {
//        if (accumulation.remainRead() > (accumulation.capacity() >> 1))
//        {
//            return;
//        }
        if (accumulation.refCount() > 1 || accumulation.capacity() > 1024 * 16)
        {
            IoBuffer newAcc = allocator.ioBuffer(accumulation.remainRead() + 512);
            newAcc.put(accumulation);
            accumulation.free();
            accumulation = newAcc;
        }
        else
        {
            accumulation.compact();
        }
    }

    @Override
    public void channelClose(ReadProcessorNode next, Throwable e)
    {
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        next.fireChannelClose(e);
    }
}
