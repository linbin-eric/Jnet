package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import lombok.Data;

@Data
public class WriteRunInWorker implements WriteProcessor<Object>
{
    private final JnetWorker worker;

    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        worker.submit(() -> next.fireWrite(data));
    }

    @Override
    public void writeFailed(WriteProcessorNode next, Throwable e)
    {
        worker.submit(() -> next.fireWriteFailed(e));
    }

    @Override
    public void channelClosed(WriteProcessorNode next)
    {
        worker.submit(() -> next.fireChannelClosed());
    }
}
