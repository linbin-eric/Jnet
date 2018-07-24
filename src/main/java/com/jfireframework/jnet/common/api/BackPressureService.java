package com.jfireframework.jnet.common.api;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public interface BackPressureService
{
    /**
     * 
     * @param data 投递失败的数据
     * @param next 触发背压
     * @param readCompletionHandler
     */
    void submit(ChannelContext channelContext, Object data, ProcessorInvoker next, ReadCompletionHandler readCompletionHandler);
    
    BackPressureService DEFAULT = new BackPressureService() {
        ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
        
        @Override
        public void submit(final ChannelContext channelContext, final Object data, final ProcessorInvoker next, final ReadCompletionHandler readCompletionHandler)
        {
            EXECUTOR_SERVICE.submit(new Runnable() {
                
                @Override
                public void run()
                {
                    try
                    {
                        while (next.process(data) == false)
                        {
                            Thread.yield();
                        }
                        readCompletionHandler.continueRead();
                    }
                    catch (Throwable e)
                    {
                        channelContext.close(e);
                    }
                }
            });
        }
        
    };
}
