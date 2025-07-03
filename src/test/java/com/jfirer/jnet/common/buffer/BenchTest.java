package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

public class BenchTest
{
    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder().include(BenchTest.class.getSimpleName()).threads(4).forks(2).build();
        new Runner(opt).run();
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
    @Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
    public void testPooledBufferAllocator_baseline(TestForPooledBufferAllocator test, Blackhole blackhole)
    {
    }

    @BenchmarkMode(Mode.Throughput)
    @Benchmark
    @Measurement(iterations = 3, time = 3, timeUnit = TimeUnit.SECONDS)
    @Warmup(iterations = 1, time = 1, timeUnit = TimeUnit.SECONDS)
    public IoBuffer testPooledBufferAllocator(TestForPooledBufferAllocator test)
    {
        IoBuffer ioBuffer = test.allocator.allocate(10000);
        ioBuffer.free();
        return ioBuffer;
    }

    @State(Scope.Benchmark)
    public static class TestForPooledBufferAllocator
    {
        public PooledBufferAllocator allocator = new PooledBufferAllocator(100, true, PooledBufferAllocator.getArena(true));
    }
}
