package com.jfireframework.jnet.common.buffer;

import java.util.Queue;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCQueue;

public class BatchRecycler implements Runnable
{
    private static final int        IDLE           = 0;
    private static final int        WORK           = 1;
    private static final int        SPIN_THRESHOLD = 1 << 7;
    private final ExecutorService   executorService;
    private final Queue<PooledIoBuffer>   entities       = new MPSCQueue<>();
    private final CpuCachePadingInt status         = new CpuCachePadingInt(IDLE);
    private final int               BATCH_NUM      = 8;
    private final IoBuffer[]        BATCH_ARRAY    = new IoBuffer[BATCH_NUM];
    private final Archon            archon;
    
    public BatchRecycler(ExecutorService executorService, Archon archon)
    {
        this.executorService = executorService;
        this.archon = archon;
    }
    
    @Override
    public void run()
    {
        int spin = 0;
        do
        {
            IoBuffer buffer = entities.poll();
            if (buffer == null)
            {
                spin = 0;
                for (;;)
                {
                    
                    if ((buffer = entities.poll()) != null)
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
                        if (entities.isEmpty() == false)
                        {
                            tryExecute();
                        }
                        return;
                    }
                }
            }
            BATCH_ARRAY[0] = buffer;
            int len = 1;
            while (len < BATCH_NUM && (buffer = entities.poll()) != null)
            {
                BATCH_ARRAY[len] = buffer;
                len += 1;
            }
            archon.recycle(BATCH_ARRAY, 0, len);
            for (int i = 0; i < len; i++)
            {
                BATCH_ARRAY[i] = null;
            }
        } while (true);
    }
    
    public void commit(PooledIoBuffer buffer)
    {
        entities.add(buffer);
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
