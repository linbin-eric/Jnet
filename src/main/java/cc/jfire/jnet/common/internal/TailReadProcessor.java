package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import lombok.Data;

@Data
public class TailReadProcessor implements ReadProcessor<Object>
{
    private final AdaptiveReadCompletionHandler completionHandler;

    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        if (data != null)
        {
            throw new IllegalArgumentException();
        }
        completionHandler.registerRead();
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
