package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.IoBuffer;
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
		IoBuffer handler = IoBuffer.heapIoBuffer();
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
		IoBuffer handler = IoBuffer.directBuffer();
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
		IoBuffer buffer1 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer1, null);
		IoBuffer buffer2 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer2, null);
		IoBuffer buffer3 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer3, null);
		IoBuffer buffer4 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer4, null);
		IoBuffer buffer5 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer5, null);
		IoBuffer buffer6 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer6, null);
		IoBuffer buffer7 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer7, null);
		IoBuffer buffer8 = IoBuffer.heapIoBuffer();
		chunk.apply(1, buffer8, null);
		chunk.recycle(buffer2);
		chunk.recycle(buffer4);
		chunk.recycle(buffer6);
		chunk.recycle(buffer8);
		IoBuffer buffer = IoBuffer.heapIoBuffer();
		boolean apply = chunk.apply(2, buffer, null);
		assertTrue(apply);
	}
}
