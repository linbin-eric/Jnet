package cc.jfire.jnet.common.buffer;

import cc.jfire.jnet.common.buffer.allocator.BufferAllocator;
import cc.jfire.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import org.jctools.queues.SpscLinkedQueue;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class BenchCache
{
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

    @Benchmark
    @Group("pool")
    @GroupThreads(1)
    public IoBuffer testPooledBufferGet(Data data)
    {
        IoBuffer ioBuffer = data.pooled.allocate(100);
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

    @State(Scope.Group)
    public static class Data
    {
        BufferAllocator           pooled      = new PooledBufferAllocator(100, true, PooledBufferAllocator.getArena(true));
        SpscLinkedQueue<IoBuffer> pooledQueue = new SpscLinkedQueue<>();
        SpscLinkedQueue<IoBuffer> cachedQueue = new SpscLinkedQueue<>();
    }
}
