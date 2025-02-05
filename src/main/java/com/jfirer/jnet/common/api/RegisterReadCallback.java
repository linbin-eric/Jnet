package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;

public interface RegisterReadCallback
{
    RegisterReadCallback INSTANCE = new RegisterReadCallback()
    {
    };

    default void onRegister(AdaptiveReadCompletionHandler readCompletionHandler, Pipeline pipeline)
    {
        readCompletionHandler.registerRead();
    }
}
