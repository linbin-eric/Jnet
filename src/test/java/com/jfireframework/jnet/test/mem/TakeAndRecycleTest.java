package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.Handler;
import com.jfireframework.jnet.common.mem.handler.HeapHandler;

public class TakeAndRecycleTest
{
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk<byte[]> pooledMem = new HeapChunk(4, 128);
		Handler<byte[]> handler = new HeapHandler();
		pooledMem.apply(400, handler,null);
		assertEquals(4, handler.getIndex());
		Handler<byte[]> handler2 = new HeapHandler();
		pooledMem.apply(100, handler2, null);
		assertEquals(20, handler2.getIndex());
		Handler<byte[]> handler3 = new HeapHandler();
		pooledMem.apply(200, handler3, null);
		assertEquals(11, handler3.getIndex());
		Handler<byte[]> handler4 = new HeapHandler();
		pooledMem.apply(100, handler4, null);
		assertEquals(21, handler4.getIndex());
		Handler<byte[]> handler5 = new HeapHandler();
		pooledMem.apply(500, handler5, null);
		assertEquals(6, handler5.getIndex());
		pooledMem.recycle(handler3);
		pooledMem.recycle(handler4);
		pooledMem.recycle(handler2);
		Handler<byte[]> handler6 = new HeapHandler();
		pooledMem.apply(400, handler6, null);
		assertEquals(5, handler6.getIndex());
	}
}
