package com.jfirer.jnet.common.buffer;

public class BenchRwTest
{
//    @State(Scope.Benchmark)
//    public static class BufferWrapper
//    {
//        UnPoolUnsafeBuffer unPoolUnsafeBuffer;
//        UnPoolDirectBuffer unPoolDirectBuffer;
//        BasicBuffer   unPoolHeapBuffer;
//        UnPoolMemoryBuffer unPoolMemoryBuffer;
//        ByteBuffer         buffer     = ByteBuffer.allocateDirect(10);
//        ByteBuffer         heapBuffer = ByteBuffer.allocate(10);
//
//        @Setup(Level.Trial)
//        public void before()
//        {
//            UnPoolBufferAllocator allocator = new UnPoolBufferAllocator();
//            unPoolUnsafeBuffer = allocator.unsafeBuffer(10);
//            unPoolDirectBuffer = allocator.directByteBuffer(10);
//            unPoolHeapBuffer = (BasicBuffer) allocator.heapBuffer(10);
//            unPoolMemoryBuffer = allocator.memoryBuffer(10);
//        }
//    }
//
//    @Benchmark
//    public void baseline()
//    {
//    }
//
//    @Benchmark
//    public IoBuffer<ByteBuffer> unsafe(BufferWrapper wrapper)
//    {
//        UnPoolUnsafeBuffer buffer = wrapper.unPoolUnsafeBuffer;
//        buffer.put((byte) 1, 0);
//        return buffer;
//    }
//
//    //    @Benchmark
//    public BasicBuffer heap(BufferWrapper wrapper)
//    {
//        BasicBuffer buffer = wrapper.unPoolHeapBuffer;
//        buffer.put((byte) 1, 0);
//        return buffer;
//    }
//
//    @Benchmark
//    public UnPoolDirectBuffer byteBuffer(BufferWrapper wrapper)
//    {
//        UnPoolDirectBuffer buffer = wrapper.unPoolDirectBuffer;
//        buffer.put((byte) 1, 0);
//        return buffer;
//    }
//
//    @Benchmark
//    public UnPoolMemoryBuffer memoryBuffer(BufferWrapper wrapper)
//    {
//        UnPoolMemoryBuffer buffer = wrapper.unPoolMemoryBuffer;
//        buffer.put((byte) 1, 0);
//        return buffer;
//    }
//
//    @Benchmark
//    public ByteBuffer originBuffer(BufferWrapper wrapper)
//    {
//        ByteBuffer buffer = wrapper.buffer;
//        buffer.put(0, (byte) 1);
//        return buffer;
//    }
//
//    //    @Benchmark
//    public ByteBuffer originHeapBuffer(BufferWrapper wrapper)
//    {
//        ByteBuffer heapBuffer = wrapper.heapBuffer;
//        heapBuffer.put(0, (byte) 1);
//        return heapBuffer;
//    }
//
//    public static void main(String[] args) throws RunnerException
//    {
//        Options opt = new OptionsBuilder().include(BenchRwTest.class.getSimpleName())//
//                                          .warmupIterations(1).warmupTime(TimeValue.seconds(3))//
//                                          .measurementIterations(5).measurementTime(TimeValue.seconds(1))//
//                                          .threads(1).forks(1).build();
//        new Runner(opt).run();
//    }
}
