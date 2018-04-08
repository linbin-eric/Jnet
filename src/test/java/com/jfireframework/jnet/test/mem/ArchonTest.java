package com.jfireframework.jnet.test.mem;

import java.lang.reflect.Field;
import org.junit.Assert;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.HeapPooledArchon;
import com.jfireframework.jnet.common.mem.archon.PooledArchon;
import com.jfireframework.jnet.common.mem.chunk.Chunk;
import com.jfireframework.jnet.common.mem.chunk.ChunkList;
import com.jfireframework.jnet.common.mem.handler.HeapHandler;

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
		HeapHandler handler = new HeapHandler();
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
}
