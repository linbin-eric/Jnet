package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WorkerGroup;

import java.util.concurrent.atomic.AtomicInteger;

public class DefaultWorkerGroup implements WorkerGroup
{
    private JnetWorker[]  workers;
    private int           numOfWorker;
    private AtomicInteger count = new AtomicInteger();

    public DefaultWorkerGroup(int numOfWorker, String namePrefix)
    {
        this.numOfWorker = numOfWorker;
        workers          = new JnetWorker[numOfWorker];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = new JnetWorkerImpl(namePrefix + i);
            ((JnetWorkerImpl) workers[i]).start();
        }
    }

    @Override
    public JnetWorker next()
    {
        return workers[count.getAndIncrement() % numOfWorker];
    }
}
