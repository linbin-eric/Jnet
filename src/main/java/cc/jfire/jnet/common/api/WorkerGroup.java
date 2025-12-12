package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.internal.JnetWorkerImpl;

public interface WorkerGroup
{
    static JnetWorker next(Thread.UncaughtExceptionHandler uncaughtExceptionHandler)
    {
        return new JnetWorkerImpl(uncaughtExceptionHandler);
    }

    static JnetWorker next()
    {
        return new JnetWorkerImpl((t, e) -> {
            System.err.println("Some RunnableImpl run in Jnet not handle Exception well,Check all ReadProcessor and WriteProcessor");
            e.printStackTrace();
        });
    }
}
