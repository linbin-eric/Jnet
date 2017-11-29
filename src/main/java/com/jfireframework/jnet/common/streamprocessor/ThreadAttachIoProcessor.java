package com.jfireframework.jnet.common.streamprocessor;

import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.streamprocessor.worker.ThreadAttachWorker;

public class ThreadAttachIoProcessor implements ReadProcessor
{
    private final ExecutorService                        executorService;
    private static final ThreadLocal<ThreadAttachWorker> localWorkers = new ThreadLocal<>();
    
    public ThreadAttachIoProcessor(ExecutorService executorService)
    {
        this.executorService = executorService;
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        ThreadAttachWorker worker = localWorkers.get();
        if (worker == null)
        {
            worker = new ThreadAttachWorker();
            executorService.execute(worker);
            localWorkers.set(worker);
        }
        worker.commit(chain, data, channelContext);
        
    }
    
}
