package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.internal.AdaptiveReadCompletionHandler;

public interface ReadListener
{
    ReadListener INSTANCE = new ReadListener()
    {
    };

    /**
     * 当需要注册读取的时候触发该方法
     * @param readCompletionHandler
     * @param pipeline
     */
    default void onNeedRegister(AdaptiveReadCompletionHandler readCompletionHandler, Pipeline pipeline)
    {
        readCompletionHandler.registerRead();
    }
}
