package com.jfirer.jnet.common.buffer;

import com.jfirer.jnet.common.buffer.allocator.impl.UnPoolBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolDirectByteBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolHeapBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.unpool.UnPoolMemoryBuffer;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.nio.ByteBuffer;

public class BenchRwTest
{
    @State(Scope.Benchmark)
    public static class BufferWrapper
    {
        UnPoolDirectBuffer     unPoolDirectBuffer;
        UnPoolDirectByteBuffer unPoolDirectByteBuffer;
        UnPoolHeapBuffer       unPoolHeapBuffer;
        UnPoolMemoryBuffer     unPoolMemoryBuffer;

        @Setup(Level.Trial)
        public void before()
        {
            UnPoolBufferAllocator allocator = new UnPoolBufferAllocator();
            unPoolDirectBuffer = (UnPoolDirectBuffer) allocator.directBuffer(10);
            unPoolDirectByteBuffer = allocator.directByteBuffer(10);
            unPoolHeapBuffer = (UnPoolHeapBuffer) allocator.heapBuffer(10);
            unPoolMemoryBuffer = allocator.memoryBuffer(10);
        }
    }

    @Benchmark
    public void baseline()
    {
    }

    @Benchmark
    public IoBuffer<ByteBuffer> direct(BufferWrapper wrapper)
    {
        UnPoolDirectBuffer buffer = wrapper.unPoolDirectBuffer;
        buffer.put((byte) 1, 0);
        return buffer;
    }

    @Benchmark
    public UnPoolHeapBuffer heap(BufferWrapper wrapper)
    {
        UnPoolHeapBuffer buffer = wrapper.unPoolHeapBuffer;
        buffer.put((byte) 1, 0);
        return buffer;
    }

    @Benchmark
    public UnPoolDirectByteBuffer byteBuffer(BufferWrapper wrapper)
    {
        UnPoolDirectByteBuffer buffer = wrapper.unPoolDirectByteBuffer;
        buffer.put((byte) 1, 0);
        return buffer;
    }

    @Benchmark
    public UnPoolMemoryBuffer memoryBuffer(BufferWrapper wrapper)
    {
        UnPoolMemoryBuffer buffer = wrapper.unPoolMemoryBuffer;
        buffer.put((byte) 1, 0);
        return buffer;
    }

    public static void main(String[] args) throws RunnerException
    {
        Options opt = new OptionsBuilder().include(BenchRwTest.class.getSimpleName())//
                                          .warmupIterations(1).warmupTime(TimeValue.seconds(3))//
                                          .measurementIterations(3).measurementTime(TimeValue.seconds(2))//
                                          .threads(1).forks(2).build();
        new Runner(opt).run();
    }
}
