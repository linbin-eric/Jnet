package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;

public class TailReadProcessor implements ReadProcessor<Object>
{
    public static final TailReadProcessor INSTANCE = new TailReadProcessor();

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        ;
    }

    @Override
    public void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        ;
    }
}
