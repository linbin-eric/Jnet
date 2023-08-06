package com.jfirer.jnet;

import com.jfirer.jnet.common.buffer.*;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@SuiteClasses({ //
        ArenaTest.class, //
        BasicBufferRWTest.class,
        BufferRecycleTest.class, //
        ChunkListTest.class, //
        HugeAllocateTest.class, //
        MemoryRegionCacheSmallTest.class, //
        NormalAllocateTest.class, //
        PooledBufferRWTest.class, //
        ReAllocateTest.class, //
        SliceBufferTest.class,//
        SmallAllocateTest.class, //
        TakeAndRecycleTest.class, //
})
@RunWith(Suite.class)
public class BufferSuiteTest
{

}
