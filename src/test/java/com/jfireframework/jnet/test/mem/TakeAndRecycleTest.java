package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class TakeAndRecycleTest
{
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk pooledMem = new HeapChunk(4, 128);
		HeapIoBuffer handler = new HeapIoBuffer();
		pooledMem.apply(400, handler,null);
		assertEquals(4, handler.getIndex());
		HeapIoBuffer handler2 = new HeapIoBuffer();
		pooledMem.apply(100, handler2, null);
		assertEquals(20, handler2.getIndex());
		HeapIoBuffer handler3 = new HeapIoBuffer();
		pooledMem.apply(200, handler3, null);
		assertEquals(11, handler3.getIndex());
		HeapIoBuffer handler4 = new HeapIoBuffer();
		pooledMem.apply(100, handler4, null);
		assertEquals(21, handler4.getIndex());
		HeapIoBuffer handler5 = new HeapIoBuffer();
		pooledMem.apply(500, handler5, null);
		assertEquals(6, handler5.getIndex());
		pooledMem.recycle(handler3);
		pooledMem.recycle(handler4);
		pooledMem.recycle(handler2);
		HeapIoBuffer handler6 = new HeapIoBuffer();
		pooledMem.apply(400, handler6, null);
		assertEquals(5, handler6.getIndex());
	}
}
