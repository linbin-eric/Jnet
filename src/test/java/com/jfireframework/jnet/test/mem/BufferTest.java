package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import org.junit.Test;
import com.jfireframework.jnet.common.buffer.AbstractIoBuffer;
import com.jfireframework.jnet.common.buffer.Archon;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

public class BufferTest
{
	private Field field;
	
	public BufferTest()
	{
		try
		{
			field = AbstractIoBuffer.class.getDeclaredField("internalByteBuffer");
			field.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * heap扩容测试
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void test() throws IllegalArgumentException, IllegalAccessException
	{
		Archon archon = Archon.heapPooledArchon(4, 1);
		PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
		archon.apply(buffer, 2);
		buffer.put((byte) 0x01);
		assertEquals(1, buffer.remainWrite());
		buffer.put((byte) 0x01);
		buffer.get();
		buffer.byteBuffer();
		assertEquals(2, buffer.capacity());
		assertEquals(0, buffer.remainWrite());
		assertEquals(1, buffer.getReadPosi());
		assertTrue(isInternalByteBufferNotNull(buffer));
		buffer.put((byte) 0x01);
		assertEquals(4, buffer.capacity());
		assertEquals(1, buffer.remainWrite());
		assertEquals(1, buffer.getReadPosi());
		assertFalse(isInternalByteBufferNotNull(buffer));
		buffer.get();
		buffer.byteBuffer();
		assertTrue(isInternalByteBufferNotNull(buffer));
		buffer.put(new byte[2]);
		assertEquals(8, buffer.capacity());
		assertEquals(3, buffer.remainWrite());
		assertEquals(2, buffer.getReadPosi());
		assertFalse(isInternalByteBufferNotNull(buffer));
	}
	
	private boolean isInternalByteBufferNotNull(AbstractIoBuffer buffer)
	{
		try
		{
			return field.get(buffer) != null;
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * direct扩容测试
	 */
	@Test
	public void test2()
	{
		Archon archon = Archon.directPooledArchon(4, 1);
		PooledIoBuffer buffer = PooledIoBuffer.directBuffer();
		archon.apply(buffer, 2);
		buffer.put((byte) 0x01);
		assertEquals(1, buffer.remainWrite());
		buffer.put((byte) 0x01);
		buffer.get();
		buffer.byteBuffer();
		assertEquals(2, buffer.capacity());
		assertEquals(0, buffer.remainWrite());
		assertEquals(1, buffer.getReadPosi());
		assertTrue(isInternalByteBufferNotNull(buffer));
		buffer.put((byte) 0x01);
		assertEquals(4, buffer.capacity());
		assertEquals(1, buffer.remainWrite());
		assertEquals(1, buffer.getReadPosi());
		assertFalse(isInternalByteBufferNotNull(buffer));
		buffer.get();
		buffer.byteBuffer();
		assertTrue(isInternalByteBufferNotNull(buffer));
		buffer.put(new byte[2]);
		assertEquals(8, buffer.capacity());
		assertEquals(3, buffer.remainWrite());
		assertEquals(2, buffer.getReadPosi());
		assertFalse(isInternalByteBufferNotNull(buffer));
	}
	
	/**
	 * buffer的获取测试
	 */
	@Test
	public void test3()
	{
		Chunk chunk = Chunk.newHeapChunk(4, 1);
		PooledIoBuffer buffer1 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer1, false);
		PooledIoBuffer buffer2 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer2, false);
		PooledIoBuffer buffer3 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer3, false);
		PooledIoBuffer buffer4 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer4, false);
		PooledIoBuffer buffer5 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer5, false);
		PooledIoBuffer buffer6 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer6, false);
		PooledIoBuffer buffer7 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer7, false);
		PooledIoBuffer buffer8 = PooledIoBuffer.heapBuffer();
		chunk.apply(1, buffer8, false);
		chunk.recycle(buffer2.index());
		chunk.recycle(buffer4.index());
		chunk.recycle(buffer6.index());
		chunk.recycle(buffer8.index());
		PooledIoBuffer buffer = PooledIoBuffer.heapBuffer();
		boolean apply = chunk.apply(2, buffer, false);
		assertTrue(apply);
	}
}
