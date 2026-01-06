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
        // 如果数据传递到末端处理器，说明处理链存在问题，应立即抛出异常
        throw new IllegalStateException("数据不应该传递到 TailReadProcessor，请检查处理链配置");
    }

    @Override
    public void readCompleted(ReadProcessorNode next)
    {
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
