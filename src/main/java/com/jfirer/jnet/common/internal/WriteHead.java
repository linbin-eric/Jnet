package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import lombok.Data;

@Data
class WriteHead implements WriteProcessorNode
{
    private final JnetWorker         worker;
    private       WriteProcessorNode next;

    public WriteHead(JnetWorker worker)
    {
        this.worker = worker;
    }

    @Override
    public void fireWrite(Object data)
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireWrite(data);
        }
        else
        {
            worker.submit(() -> next.fireWrite(data));
        }
    }

    @Override
    public void fireWriteClose()
    {
        if (Thread.currentThread() == worker.thread())
        {
            next.fireWriteClose();
        }
        else
        {
            worker.submit(() -> next.fireWriteClose());
        }
    }
}
