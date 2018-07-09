package com.jfireframework.jnet;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import com.jfireframework.jnet.common.buffer.ArenaTest;
import com.jfireframework.jnet.common.buffer.HugeAllocateTest;
import com.jfireframework.jnet.common.buffer.NormalAllocateTest;
import com.jfireframework.jnet.common.buffer.PooledBufferRWTest;
import com.jfireframework.jnet.common.buffer.SmallAllocateTest;
import com.jfireframework.jnet.common.buffer.TakeAndRecycleTest;
import com.jfireframework.jnet.common.buffer.TinyAllocateTest;
import com.jfireframework.jnet.common.buffer.UnPooledBufferRWTest;

@SuiteClasses({ //
        ArenaTest.class, //
        HugeAllocateTest.class, //
        NormalAllocateTest.class, //
        PooledBufferRWTest.class, //
        SmallAllocateTest.class, //
        TakeAndRecycleTest.class, //
        TinyAllocateTest.class, //
        UnPooledBufferRWTest.class, //
})
@RunWith(Suite.class)
public class BuffetSuiteTest
{
	
}
