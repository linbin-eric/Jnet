package com.jfirer.jnet;

import com.jfirer.jnet.common.buffer.*;
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
