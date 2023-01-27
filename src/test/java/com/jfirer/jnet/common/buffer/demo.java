package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.CachedPooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import org.jctools.queues.SpscLinkedQueue;

public class demo
{
    public static void main(String[] args) throws InterruptedException
    {
        BufferAllocator           cached      = new CachedPooledBufferAllocator("cached");
        SpscLinkedQueue<IoBuffer> cachedQueue = new SpscLinkedQueue<>();
        Runnable get = () -> {
            for (long i = 0; i < 10000000000L; i++)
            {
                IoBuffer ioBuffer = cached.ioBuffer(100);
                cachedQueue.offer(ioBuffer);
//                if (i % 1000000==0)
//                {
//                    System.out.println(cachedQueue.size());
//                }
            }
        };
        Runnable free = () -> {
            for (long i = 0; i < 10000000000L; i++)
            {
                IoBuffer poll = cachedQueue.poll();
                if (poll != null)
                {
                    poll.free();
                }
            }
        };
        Thread t1 = new FastThreadLocalThread(get);
        Thread t2 = new FastThreadLocalThread(free);
        t1.start();
        t2.start();
        t2.join();
    }
}
