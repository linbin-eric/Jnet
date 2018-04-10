package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import org.junit.Assert;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.DirectPooledArchon;
import com.jfireframework.jnet.common.mem.archon.HeapPooledArchon;
import com.jfireframework.jnet.common.mem.archon.PooledArchon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.ChunkList;
import com.jfireframework.jnet.common.mem.handler.AbstractIoBuffer;
import com.jfireframework.jnet.common.mem.handler.DirectIoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class ArchonTest
{
	@SuppressWarnings("unchecked")
	@Test
	public void test() throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException
	{
		Archon<byte[]> archon = new HeapPooledArchon(4, 1);
		Field field = PooledArchon.class.getDeclaredField("cInt");
		field.setAccessible(true);
		ChunkList<byte[]> cint = (ChunkList<byte[]>) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c25");
		field.setAccessible(true);
		ChunkList<byte[]> c25 = (ChunkList<byte[]>) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c50");
		field.setAccessible(true);
		ChunkList<byte[]> c50 = (ChunkList<byte[]>) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c75");
		field.setAccessible(true);
		ChunkList<byte[]> c75 = (ChunkList<byte[]>) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c100");
		field.setAccessible(true);
		ChunkList<byte[]> c100 = (ChunkList<byte[]>) field.get(archon);
		field = PooledArchon.class.getDeclaredField("c000");
		field.setAccessible(true);
		ChunkList<byte[]> c000 = (ChunkList<byte[]>) field.get(archon);
		HeapIoBuffer handler = new HeapIoBuffer();
		Field headField = ChunkList.class.getDeclaredField("head");
		headField.setAccessible(true);
		archon.apply(1, handler);
		archon.apply(1, handler);
		archon.apply(1, handler);
		Assert.assertTrue(headField.get(cint) != null);
		Assert.assertTrue(headField.get(c25) == null);
		archon.apply(1, handler);
		archon.apply(1, handler);
		Assert.assertEquals(31, ((Chunk<?>) headField.get(c000)).usage());
		archon.apply(1, handler);
		archon.apply(1, handler);
		Assert.assertNull(headField.get(cint));
		Assert.assertNotNull(headField.get(c000));
		archon.apply(1, handler);
		Assert.assertNull(headField.get(c000));
		Assert.assertEquals(50, ((Chunk<?>) headField.get(c25)).usage());
		archon.apply(1, handler);
		archon.apply(1, handler);
		Assert.assertNotNull(headField.get(c25));
		Assert.assertNull(headField.get(c75));
		archon.apply(1, handler);
		archon.apply(1, handler);
		archon.apply(1, handler);
		archon.apply(1, handler);
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
		Archon<byte[]> archon = new HeapPooledArchon(4, 1);
		HeapIoBuffer handler = new HeapIoBuffer();
		archon.apply(2, handler);
		handler.put((byte) 0x01);
		handler.put((byte) 0x02);
		handler.get();
		Field field = AbstractIoBuffer.class.getDeclaredField("chunk");
		field.setAccessible(true);
		Chunk<byte[]> originChunk = (Chunk<byte[]>) field.get(handler);
		int originCapacity = handler.capacity();
		int originReadPosi = handler.getReadPosi();
		int originWritePosi = handler.getWritePosi();
		int originIndex = handler.getIndex();
		assertEquals(2, originCapacity);
		assertEquals(8, originIndex);
		assertEquals(1, originReadPosi);
		assertEquals(2, originWritePosi);
		archon.expansion(handler, 4);
		assertEquals(5, handler.getIndex());
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.getReadPosi());
		assertEquals(2, handler.getWritePosi());
		assertTrue(handler.get(0) == 0x01);
		assertTrue(handler.get(1) == 0x02);
		Chunk<byte[]> chunk = (Chunk<byte[]>) field.get(handler);
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
		Archon<ByteBuffer> archon = new DirectPooledArchon(4, 1);
		DirectIoBuffer handler = new DirectIoBuffer();
		archon.apply(2, handler);
		handler.put((byte) 0x01);
		handler.put((byte) 0x02);
		handler.get();
		Field field = AbstractIoBuffer.class.getDeclaredField("chunk");
		field.setAccessible(true);
		Chunk<byte[]> originChunk = (Chunk<byte[]>) field.get(handler);
		int originCapacity = handler.capacity();
		int originReadPosi = handler.getReadPosi();
		int originWritePosi = handler.getWritePosi();
		int originIndex = handler.getIndex();
		assertEquals(2, originCapacity);
		assertEquals(8, originIndex);
		assertEquals(1, originReadPosi);
		assertEquals(2, originWritePosi);
		archon.expansion(handler, 4);
		assertEquals(5, handler.getIndex());
		assertEquals(4, handler.capacity());
		assertEquals(1, handler.getReadPosi());
		assertEquals(2, handler.getWritePosi());
		assertTrue(handler.get(0) == 0x01);
		assertTrue(handler.get(1) == 0x02);
		Chunk<byte[]> chunk = (Chunk<byte[]>) field.get(handler);
		assertTrue(originChunk == chunk);
	}
	
}
