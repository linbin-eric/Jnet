package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;

public interface RegisterReadListener
{
    RegisterReadListener INSTANCE = new RegisterReadListener()
    {
    };

    default void onRegister(AdaptiveReadCompletionHandler readCompletionHandler, Pipeline pipeline)
    {
        readCompletionHandler.registerRead();
    }
}
