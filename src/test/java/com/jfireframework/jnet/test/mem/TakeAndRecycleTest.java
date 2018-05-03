package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

public class TakeAndRecycleTest
{
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk pooledMem = Chunk.newHeapChunk(4, 128);
		PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
		pooledMem.apply(400, buffer, false);
		assertEquals(4, buffer.index());
		PooledIoBuffer buffer2 = PooledIoBuffer.heapBuffer();
		pooledMem.apply(100, buffer2, false);
		assertEquals(20, buffer2.index());
		PooledIoBuffer buffer3 = PooledIoBuffer.heapBuffer();
		pooledMem.apply(200, buffer3, false);
		assertEquals(11, buffer3.index());
		PooledIoBuffer buffer4 = PooledIoBuffer.heapBuffer();
		pooledMem.apply(100, buffer4, false);
		assertEquals(21, buffer4.index());
		PooledIoBuffer buffer5 = PooledIoBuffer.heapBuffer();
		pooledMem.apply(500, buffer5, false);
		assertEquals(6, buffer5.index());
		pooledMem.recycle(buffer3.index());
		pooledMem.recycle(buffer4.index());
		pooledMem.recycle(buffer2.index());
		PooledIoBuffer buffer6 = PooledIoBuffer.heapBuffer();
		pooledMem.apply(400, buffer6, false);
		assertEquals(5, buffer6.index());
	}
}
