package com.jfireframework.jnet;

import com.jfireframework.jnet.common.buffer.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({ //
        ArenaTest.class, //
        HugeAllocateTest.class, //
        NormalAllocateTest.class, //
        PooledBufferRWTest.class, //
        SmallAllocateTest.class, //
        TakeAndRecycleTest.class, //
        TinyAllocateTest.class, //
        ChunkListTest.class, //
        BufferRecycleTest.class, //
        ReAllocateTest.class, //
        MemoryRegionCacheTinyTest.class, //
        MemoryRegionCacheSmallTest.class, //
        MemoryRegionCacheNormalTest.class, //
        UnPooledBufferRWTest.class, //
        SliceBufferTest.class,//
})
@RunWith(Suite.class)
public class BufferSuiteTest
{

}
