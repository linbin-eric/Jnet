package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.CachedPooledBufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import org.jctools.queues.SpscLinkedQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class BenchCache
{
    @State(Scope.Group)
    public static class Data
    {
        BufferAllocator           pooled      = new PooledBufferAllocator("pool");
        BufferAllocator           cached      = new CachedPooledBufferAllocator("cached");
        SpscLinkedQueue<IoBuffer> pooledQueue = new SpscLinkedQueue<>();
        SpscLinkedQueue<IoBuffer> cachedQueue = new SpscLinkedQueue<>();
    }

    @Benchmark
    @Group("pool")
    @GroupThreads(1)
    public IoBuffer testPooledBufferGet(Data data)
    {
        IoBuffer ioBuffer = data.pooled.ioBuffer(100);
        data.pooledQueue.offer(ioBuffer);
        return ioBuffer;
    }

    @Benchmark
    @Group("pool")
    @GroupThreads(1)
    public IoBuffer testPooledBufferFree(Data data)
    {
        IoBuffer poll = data.pooledQueue.relaxedPoll();
        if (poll != null)
        {
            poll.free();
            return poll;
        }
        return poll;
    }

    @Benchmark
    @Group("cache")
    @GroupThreads(1)
    public IoBuffer testCachedBufferGet(Data data)
    {
        IoBuffer ioBuffer = data.cached.ioBuffer(100);
        data.cachedQueue.offer(ioBuffer);
        return ioBuffer;
    }

    @Benchmark
    @Group("cache")
    @GroupThreads(1)
    public IoBuffer testCachedBufferFree(Data data)
    {
        IoBuffer poll = data.cachedQueue.relaxedPoll();
        if (poll != null)
        {
            poll.free();
            return poll;
        }
        return poll;
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder().include(BenchCache.class.getSimpleName()).threads(2).forks(2)//
                                          .mode(Mode.Throughput)//
                                          .measurementIterations(3)//
                                          .measurementTime(TimeValue.seconds(10))//
                                          .warmupIterations(1)//
                                          .warmupTime(TimeValue.seconds(3))//
                                          .build();
        new Runner(opt).run();
    }
}
