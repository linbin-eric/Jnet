package cc.jfire.jnet.common.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

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
            if (accumulation != null)
            {
                accumulation.free();
                accumulation = null;
            }
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
