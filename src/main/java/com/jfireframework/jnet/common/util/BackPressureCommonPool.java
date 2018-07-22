package com.jfireframework.jnet.common.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public class BackPressureCommonPool
{
    public static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    
    public static void submit(final IoBuffer buffer, final WriteCompletionHandler writeCompletionHandler, final ReadCompletionHandler readCompletionHandler)
    {
        EXECUTOR_SERVICE.submit(new Runnable() {
            
            @Override
            public void run()
            {
                while (writeCompletionHandler.backPressureOffer(buffer, false) == false)
                {
                    Thread.yield();
                }
                readCompletionHandler.continueRead();
            }
        });
    }
}
