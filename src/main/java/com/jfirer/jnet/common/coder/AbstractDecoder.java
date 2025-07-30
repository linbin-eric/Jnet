package com.jfirer.jnet.common.coder;

import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractDecoder implements ReadProcessor<IoBuffer>
{
    protected IoBuffer accumulation;

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
            log.error("解码过程中发生未知异常", e);
            next.pipeline().shutdownInput();
        }
    }

    protected abstract void process0(ReadProcessorNode next);

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        next.fireReadFailed(e);
    }
}
