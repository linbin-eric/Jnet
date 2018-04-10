package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.HeapChunk;
import com.jfireframework.jnet.common.mem.handler.IoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class TakeAndRecycleTest
{
	
	/**
	 * 使用一维数组来表达平衡二叉树
	 */
	@Test
	public void test2()
	{
		Chunk<byte[]> pooledMem = new HeapChunk(4, 128);
		IoBuffer<byte[]> handler = new HeapIoBuffer();
		pooledMem.apply(400, handler,null);
		assertEquals(4, handler.getIndex());
		IoBuffer<byte[]> handler2 = new HeapIoBuffer();
		pooledMem.apply(100, handler2, null);
		assertEquals(20, handler2.getIndex());
		IoBuffer<byte[]> handler3 = new HeapIoBuffer();
		pooledMem.apply(200, handler3, null);
		assertEquals(11, handler3.getIndex());
		IoBuffer<byte[]> handler4 = new HeapIoBuffer();
		pooledMem.apply(100, handler4, null);
		assertEquals(21, handler4.getIndex());
		IoBuffer<byte[]> handler5 = new HeapIoBuffer();
		pooledMem.apply(500, handler5, null);
		assertEquals(6, handler5.getIndex());
		pooledMem.recycle(handler3);
		pooledMem.recycle(handler4);
		pooledMem.recycle(handler2);
		IoBuffer<byte[]> handler6 = new HeapIoBuffer();
		pooledMem.apply(400, handler6, null);
		assertEquals(5, handler6.getIndex());
	}
}
