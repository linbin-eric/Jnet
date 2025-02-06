package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.JnetWorker;
import com.jfirer.jnet.common.api.WorkerGroup;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class DefaultWorkerGroup implements WorkerGroup
{
    private final JnetWorker[] workers;
    private final int          numOfWorker;
    private final AtomicInteger count = new AtomicInteger();

    public DefaultWorkerGroup(int numOfWorker, String namePrefix)
    {
        this(numOfWorker, namePrefix, e -> {
            System.err.println("Some RunnableImpl run in Jnet not handle Exception well,Check all ReadProcessor and WriteProcessor");
            e.printStackTrace();
        });
    }

    public DefaultWorkerGroup(int numOfWorker, String namePrefix, Consumer<Throwable> jvmExistHandler)
    {
        this.numOfWorker = numOfWorker;
        workers          = new JnetWorker[numOfWorker];
        for (int i = 0; i < workers.length; i++)
        {
            workers[i] = new JnetWorkerImpl(namePrefix + i, jvmExistHandler);
            ((JnetWorkerImpl) workers[i]).start();
        }
    }

    @Override
    public JnetWorker next()
    {
        return workers[count.getAndIncrement() % numOfWorker];
    }
}
