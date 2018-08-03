package com.jfireframework.jnet.common.buffer;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class GlobalMetric
{
    
    private static final ReferenceQueue<ThreadCache> QUEUE = new ReferenceQueue<>();
    static
    {
        new Thread(new Runnable() {
            
            @Override
            public void run()
            {
                while (true)
                {
                    try
                    {
                        NumThreadCacheReference remove = (NumThreadCacheReference) QUEUE.remove();
                        remove.decrement();
                    }
                    catch (InterruptedException e)
                    {
                        ;
                    }
                }
            }
        }, "Arena Num Watcher Thread").start();
    }
    
    public static final class NumThreadCacheReference extends PhantomReference<ThreadCache>
    {
        private final AtomicInteger heapArenaNumThreadCache;
        private final AtomicInteger directArenaNumThreadCache;
        
        public NumThreadCacheReference(ThreadCache referent, ReferenceQueue<? super ThreadCache> q)
        {
            super(referent, q);
            heapArenaNumThreadCache = referent.heapArena == null ? null : referent.heapArena.numThreadCaches;
            directArenaNumThreadCache = referent.directArena == null ? null : referent.directArena.numThreadCaches;
        }
        
        public void decrement()
        {
            if (heapArenaNumThreadCache != null)
            {
                heapArenaNumThreadCache.decrementAndGet();
            }
            if (directArenaNumThreadCache != null)
            {
                directArenaNumThreadCache.decrementAndGet();
            }
        }
    }
    
    public static final void watchThreadCache(ThreadCache threadCache)
    {
        new NumThreadCacheReference(threadCache, QUEUE);
    }
}
