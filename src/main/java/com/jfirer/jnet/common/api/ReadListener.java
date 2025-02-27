package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.internal.AdaptiveReadCompletionHandler;

public interface ReadListener
{
    ReadListener INSTANCE = new ReadListener()
    {
    };

    default void onRegister(AdaptiveReadCompletionHandler readCompletionHandler, Pipeline pipeline)
    {
        readCompletionHandler.registerRead();
    }
}
