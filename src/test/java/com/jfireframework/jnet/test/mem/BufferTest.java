package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.AbstractIoBuffer;
import com.jfireframework.jnet.common.buffer.PooledArchon;

public class BufferTest
{
	
	/**
	 * heap扩容测试
	 */
	@Test
	public void test()
	{
		Archon archon = PooledArchon.heapPooledArchon(4, 1);
		AbstractIoBuffer handler = AbstractIoBuffer.heapIoBuffer();
		archon.apply(2, handler);
		handler.put((byte) 0x01);
		assertEquals(1, handler.remainWrite());
		handler.put((byte) 0x01);
		handler.get();
		assertEquals(2, handler.capacity());
		assertEquals(0, handler.remainWrite());
		assertEquals(1, handler.getReadPosi());
		handler.put((byte) 0x01);
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.remainWrite());
		assertEquals(1, handler.getReadPosi());
		handler.get();
		handler.put(new byte[2]);
		assertEquals(8, handler.capacity());
		assertEquals(3, handler.remainWrite());
		assertEquals(2, handler.getReadPosi());
	}
	
	/**
	 * direct扩容测试
	 */
	@Test
	public void test2()
	{
		Archon archon = PooledArchon.directPooledArchon(4, 1);
		AbstractIoBuffer handler = AbstractIoBuffer.directBuffer();
		archon.apply(2, handler);
		handler.put((byte) 0x01);
		assertEquals(1, handler.remainWrite());
		handler.put((byte) 0x01);
		handler.get();
		assertEquals(2, handler.capacity());
		assertEquals(0, handler.remainWrite());
		assertEquals(1, handler.getReadPosi());
		handler.put((byte) 0x01);
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.remainWrite());
		assertEquals(1, handler.getReadPosi());
		handler.get();
		handler.put(new byte[2]);
		assertEquals(8, handler.capacity());
		assertEquals(3, handler.remainWrite());
		assertEquals(2, handler.getReadPosi());
	}
	
	/**
	 * buffer的获取测试
	 */
	@Test
	public void test3()
	{
		Chunk chunk = Chunk.newHeapChunk(4, 1);
		AbstractIoBuffer buffer1 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer1, null);
		AbstractIoBuffer buffer2 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer2, null);
		AbstractIoBuffer buffer3 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer3, null);
		AbstractIoBuffer buffer4 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer4, null);
		AbstractIoBuffer buffer5 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer5, null);
		AbstractIoBuffer buffer6 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer6, null);
		AbstractIoBuffer buffer7 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer7, null);
		AbstractIoBuffer buffer8 = AbstractIoBuffer.heapIoBuffer();
		chunk.apply(1, buffer8, null);
		chunk.recycle(buffer2);
		chunk.recycle(buffer4);
		chunk.recycle(buffer6);
		chunk.recycle(buffer8);
		AbstractIoBuffer buffer = AbstractIoBuffer.heapIoBuffer();
		boolean apply = chunk.apply(2, buffer, null);
		assertTrue(apply);
	}
}
