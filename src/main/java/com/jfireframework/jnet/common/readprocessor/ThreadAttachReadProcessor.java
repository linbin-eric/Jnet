package com.jfireframework.jnet.common.readprocessor;

import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.businessprocessor.ThreadAttachProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ThreadAttachReadProcessor implements ReadProcessor
{
    private static final ThreadLocal<ThreadAttachProcessor> processLocal = new ThreadLocal<>();
    private final ExecutorService                           executorService;
    private final AioListener                               aioListener;
    
    public ThreadAttachReadProcessor(ExecutorService executorService, AioListener aioListener)
    {
        this.executorService = executorService;
        this.aioListener = aioListener;
    }
    
    @Override
    public void process(ByteBuf<?> buf, BufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
    {
        ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
        ThreadAttachProcessor processor = processLocal.get();
        if (processor == null)
        {
            processor = new ThreadAttachProcessor(aioListener);
            executorService.execute(processor);
            processLocal.set(processor);
        }
        processor.commit(task);
    }
    
}
