package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.internal.AdaptiveReadCompletionHandler;

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
