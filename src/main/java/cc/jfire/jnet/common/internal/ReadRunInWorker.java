package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.JnetWorker;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class ReadRunInWorker implements ReadProcessor<IoBuffer>
{
    private final JnetWorker worker;

    @Override
    public void read(IoBuffer data, ReadProcessorNode next)
    {
        worker.submit(() -> next.fireRead(data));
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        worker.submit(() -> next.fireReadFailed(e));
    }

    @Override
    public void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        worker.submit(() -> next.firePipelineComplete(pipeline));
    }
}
