package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import lombok.Data;

@Data
public class WriteHead implements WriteProcessorNode
{
    private final JnetWorker         worker;
    private final Pipeline           pipeline;
    private       WriteProcessorNode next;

    @Override
    public void fireWrite(Object data)
    {
        worker.submit(() -> next.fireWrite(data));
    }

    @Override
    public void fireWriteFailed(Throwable e)
    {
        worker.submit(() -> next.fireWriteFailed(e));
    }

    @Override
    public Pipeline pipeline()
    {
        return pipeline;
    }
}
