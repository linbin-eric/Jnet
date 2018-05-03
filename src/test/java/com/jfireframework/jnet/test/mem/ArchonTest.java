package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.ChunkList;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.buffer.PooledArchon;

public class ArchonTest
{
	@Test
	public void test() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		Archon archon = PooledArchon.heapPooledArchon(4, 1);
		Field field = PooledArchon.class.getDeclaredField("cInt");
		field.setAccessible(true);
		ChunkList cint = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c25");
		field.setAccessible(true);
		ChunkList c25 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c50");
		field.setAccessible(true);
		ChunkList c50 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c75");
		field.setAccessible(true);
		ChunkList c75 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c100");
		field.setAccessible(true);
		ChunkList c100 = (ChunkList) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c000");
		field.setAccessible(true);
		ChunkList c000 = (ChunkList) field.get(archon);
		PooledIoBuffer handler = PooledIoBuffer.heapBuffer();
		Field headField = ChunkList.class.getDeclaredField("head");
		headField.setAccessible(true);
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		Assert.assertTrue(headField.get(cint) != null);
		Assert.assertTrue(headField.get(c25) == null);
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		Assert.assertEquals(31, ((Chunk) headField.get(c000)).usage());
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		Assert.assertNull(headField.get(cint));
		Assert.assertNotNull(headField.get(c000));
		archon.apply(handler, 1);
		Assert.assertNull(headField.get(c000));
		Assert.assertEquals(50, ((Chunk) headField.get(c25)).usage());
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		Assert.assertNotNull(headField.get(c25));
		Assert.assertNull(headField.get(c75));
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		archon.apply(handler, 1);
		Assert.assertNotNull(headField.get(c50));
		Assert.assertNull(headField.get(c25));
	}
	
	/**
	 * Heap扩容测试
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void test2() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Archon archon = PooledArchon.heapPooledArchon(4, 1);
		PooledIoBuffer handler = PooledIoBuffer.heapBuffer();
		archon.apply(handler, 2);
		handler.put((byte) 0x01);
		handler.put((byte) 0x02);
		handler.get();
		Field field = PooledIoBuffer.class.getDeclaredField("chunk");
		field.setAccessible(true);
		Chunk originChunk = (Chunk) field.get(handler);
		int originCapacity = handler.capacity();
		int originReadPosi = handler.getReadPosi();
		int originWritePosi = handler.getWritePosi();
		int originIndex = handler.indexOfChunk();
		assertEquals(2, originCapacity);
		assertEquals(8, originIndex);
		assertEquals(1, originReadPosi);
		assertEquals(2, originWritePosi);
		archon.expansion(handler, 4);
		assertEquals(5, handler.indexOfChunk());
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.getReadPosi());
		assertEquals(2, handler.getWritePosi());
		assertTrue(handler.get(0) == 0x01);
		assertTrue(handler.get(1) == 0x02);
		Chunk chunk = (Chunk) field.get(handler);
		assertTrue(originChunk == chunk);
	}
	
	/**
	 * Direct扩容测试
	 * 
	 * @throws SecurityException
	 * @throws NoSuchFieldException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void test3() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException
	{
		Archon archon = PooledArchon.directPooledArchon(4, 1);
		PooledIoBuffer handler = PooledIoBuffer.directBuffer();
		archon.apply(handler, 2);
		handler.put((byte) 0x01);
		handler.put((byte) 0x02);
		handler.get();
		Field field = PooledIoBuffer.class.getDeclaredField("chunk");
		field.setAccessible(true);
		Chunk originChunk = (Chunk) field.get(handler);
		int originCapacity = handler.capacity();
		int originReadPosi = handler.getReadPosi();
		int originWritePosi = handler.getWritePosi();
		int originIndex = handler.indexOfChunk();
		assertEquals(2, originCapacity);
		assertEquals(8, originIndex);
		assertEquals(1, originReadPosi);
		assertEquals(2, originWritePosi);
		archon.expansion(handler, 4);
		assertEquals(5, handler.indexOfChunk());
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.getReadPosi());
		assertEquals(2, handler.getWritePosi());
		assertTrue(handler.get(0) == 0x01);
		assertTrue(handler.get(1) == 0x02);
		Chunk chunk = (Chunk) field.get(handler);
		assertTrue(originChunk == chunk);
	}
	
}
