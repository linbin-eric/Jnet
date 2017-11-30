package com.jfireframework.jnet.common.processor.worker;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCQueue;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;

public class ThreadAttachWorker implements Runnable
{
    private final Queue<ThreadAttachEntity> entities       = new MPSCQueue<>();
    private static final int                IDLE           = 0;
    private static final int                WORK           = 1;
    private final CpuCachePadingInt         status         = new CpuCachePadingInt(WORK);
    private static final int                SPIN_THRESHOLD = 1 << 7;
    private int                             spin           = 0;
    private volatile Thread                 owner;
    
    @Override
    public void run()
    {
        status.set(WORK);
        owner = Thread.currentThread();
        termination: //
        do
        {
            ThreadAttachEntity entity = entities.poll();
            if (entity == null)
            {
                spin = 0;
                for (;;)
                {
                    
                    if ((entity = entities.poll()) != null)
                    {
                        break;
                    }
                    else if ((spin += 1) < SPIN_THRESHOLD)
                    {
                        ;
                    }
                    else
                    {
                        spin = 0;
                        status.set(IDLE);
                        if ((entity = entities.poll()) != null)
                        {
                            status.set(WORK);
                            break;
                        }
                        else
                        {
                            LockSupport.park();
                            status.set(WORK);
                            if (Thread.currentThread().isInterrupted())
                            {
                                break termination;
                            }
                        }
                    }
                }
            }
            try
            {
                entity.chain.chain(entity.data);
            }
            catch (Throwable e)
            {
                try
                {
                    entity.channelContext.socketChannel().close();
                }
                catch (IOException e1)
                {
                    e1.printStackTrace();
                }
            }
        } while (true);
    }
    
    public void commit(ProcessorChain chain, Object data, ChannelContext channelContext)
    {
        entities.offer(new ThreadAttachEntity(channelContext, chain, data));
        tryExecute();
    }
    
    private void tryExecute()
    {
        int now = status.value();
        if (now == IDLE)
        {
            LockSupport.unpark(owner);
        }
    }
    
    class ThreadAttachEntity
    {
        ChannelContext channelContext;
        ProcessorChain chain;
        Object         data;
        
        public ThreadAttachEntity(ChannelContext channelContext, ProcessorChain chain, Object data)
        {
            this.channelContext = channelContext;
            this.chain = chain;
            this.data = data;
        }
        
    }
}
