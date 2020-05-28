package com.jfireframework.jnet.common.api;

public interface JnetWorker
{
    void submit(Runnable runnable);

    void shuwdown();

    Thread thread();
}
