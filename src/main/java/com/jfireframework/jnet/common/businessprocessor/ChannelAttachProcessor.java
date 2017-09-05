package com.jfireframework.jnet.common.businessprocessor;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.SpscQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ChannelAttachProcessor implements Runnable
{
    private final Queue<ProcessorTask> tasks          = new SpscQueue<>();
    private static final int           IDLE           = 0;
    private static final int           WORK           = 1;
    private final CpuCachePadingInt    status         = new CpuCachePadingInt(IDLE);
    private final ExecutorService      executorService;
    private final AioListener          serverListener;
    private final SendBufStorage           bufStorage;
    private final WriteHandler         writeHandler;
    private final StreamProcessor[]    inProcessors;
    private final ChannelContext       channelContext;
    private static final int           SPIN_THRESHOLD = 1 << 7;
    private int                        spin           = 0;
    
    public ChannelAttachProcessor(ExecutorService executorService, AioListener serverListener, ChannelContext channelContext, SendBufStorage bufStorage, WriteHandler writeHandler, StreamProcessor[] inProcessors)
    {
        this.executorService = executorService;
        this.serverListener = serverListener;
        this.channelContext = channelContext;
        this.bufStorage = bufStorage;
        this.writeHandler = writeHandler;
        this.inProcessors = inProcessors;
    }
    
    @Override
    public void run()
    {
        do
        {
            ProcessorTask task = tasks.poll();
            if (task == null)
            {
                spin = 0;
                for (;;)
                {
                    
                    if ((task = tasks.poll()) != null)
                    {
                        break;
                    }
                    else if ((spin += 1) < SPIN_THRESHOLD)
                    {
                        ;
                    }
                    else
                    {
                        status.set(IDLE);
                        if (tasks.isEmpty() == false)
                        {
                            tryExecute();
                        }
                        return;
                    }
                }
            }
            try
            {
                Object result = ProcesserUtil.process(channelContext, inProcessors, task.getData(), task.getInitIndex());
                if (result instanceof ByteBuf<?>)
                {
                    bufStorage.putBuf((ByteBuf<?>) result);
                    writeHandler.registerWrite();
                }
            }
            catch (Throwable e)
            {
                serverListener.catchException(e, channelContext);
                if (channelContext.isOpen() == false)
                {
                    return;
                }
            }
        } while (true);
    }
    
    public void commit(ProcessorTask task)
    {
        tasks.offer(task);
        tryExecute();
    }
    
    private void tryExecute()
    {
        int now = status.value();
        if (now == IDLE && status.compareAndSwap(IDLE, WORK))
        {
            executorService.execute(this);
        }
    }
}
