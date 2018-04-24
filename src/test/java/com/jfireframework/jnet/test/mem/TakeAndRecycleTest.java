package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.AbstractIoBuffer;

public class TakeAndRecycleTest
{
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk pooledMem = Chunk.newHeapChunk(4, 128);
		AbstractIoBuffer handler = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(400, handler, null);
		assertEquals(4, handler.getIndex());
		AbstractIoBuffer handler2 = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(100, handler2, null);
		assertEquals(20, handler2.getIndex());
		AbstractIoBuffer handler3 = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(200, handler3, null);
		assertEquals(11, handler3.getIndex());
		AbstractIoBuffer handler4 = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(100, handler4, null);
		assertEquals(21, handler4.getIndex());
		AbstractIoBuffer handler5 = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(500, handler5, null);
		assertEquals(6, handler5.getIndex());
		pooledMem.recycle(handler3);
		pooledMem.recycle(handler4);
		pooledMem.recycle(handler2);
		AbstractIoBuffer handler6 = AbstractIoBuffer.heapIoBuffer();
		pooledMem.apply(400, handler6, null);
		assertEquals(5, handler6.getIndex());
	}
}
