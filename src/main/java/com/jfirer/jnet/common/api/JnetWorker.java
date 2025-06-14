package com.jfirer.jnet.common.api;

public interface JnetWorker
{
    void submit(Runnable runnable);

    void shutdown();
}
