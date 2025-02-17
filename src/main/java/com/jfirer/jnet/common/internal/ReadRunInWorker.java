package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
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
