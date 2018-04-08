package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.Handler;
import com.jfireframework.jnet.common.mem.handler.HeapHandler;

public class TakeAndRecycleTest
{
	// @Test
	// public void test_1()
	// {
	// Chunk<byte[]> pooledMem = new HeapChunk(4, 128);
	// Handler<byte[]> handler = new HeapHandler();
	// pooledMem.apply(400, handler);
	// assertEquals(2, handler.getLevel());
	// assertEquals(0, handler.getIndex());
	// Handler<byte[]> handler2 = new HeapHandler();
	// pooledMem.apply(100, handler2);
	// assertEquals(4, handler2.getLevel());
	// assertEquals(4, handler2.getIndex());
	// Handler<byte[]> handler3 = new HeapHandler();
	// pooledMem.apply(200, handler3);
	// assertEquals(3, handler3.getLevel());
	// assertEquals(3, handler3.getIndex());
	// Handler<byte[]> handler4 = new HeapHandler();
	// pooledMem.apply(100, handler4);
	// assertEquals(4, handler4.getLevel());
	// assertEquals(5, handler4.getIndex());
	// Handler<byte[]> handler5 = new HeapHandler();
	// pooledMem.apply(500, handler5);
	// assertEquals(2, handler5.getLevel());
	// assertEquals(2, handler5.getIndex());
	// pooledMem.recycle(handler3);
	// pooledMem.recycle(handler4);
	// pooledMem.recycle(handler2);
	// Handler<byte[]> handler6 = new HeapHandler();
	// pooledMem.apply(400, handler6);
	// assertEquals(2, handler6.getLevel());
	// assertEquals(1, handler6.getIndex());
	// }
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk<byte[]> pooledMem = new HeapChunk(4, 128);
		Handler<byte[]> handler = new HeapHandler();
		pooledMem.apply(400, handler);
		assertEquals(4, handler.getIndex());
		Handler<byte[]> handler2 = new HeapHandler();
		pooledMem.apply(100, handler2);
		assertEquals(20, handler2.getIndex());
		Handler<byte[]> handler3 = new HeapHandler();
		pooledMem.apply(200, handler3);
		assertEquals(11, handler3.getIndex());
		Handler<byte[]> handler4 = new HeapHandler();
		pooledMem.apply(100, handler4);
		assertEquals(21, handler4.getIndex());
		Handler<byte[]> handler5 = new HeapHandler();
		pooledMem.apply(500, handler5);
		assertEquals(6, handler5.getIndex());
		pooledMem.recycle(handler3);
		pooledMem.recycle(handler4);
		pooledMem.recycle(handler2);
		Handler<byte[]> handler6 = new HeapHandler();
		pooledMem.apply(400, handler6);
		assertEquals(5, handler6.getIndex());
	}
}
