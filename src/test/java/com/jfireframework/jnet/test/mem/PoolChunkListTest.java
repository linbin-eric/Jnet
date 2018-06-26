package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.jnet.common.buffer2.Chunk;
import com.jfireframework.jnet.common.buffer2.ChunkList;
import com.jfireframework.jnet.common.buffer2.DirectChunk;
import com.jfireframework.jnet.common.buffer2.DirectChunkList;
import com.jfireframework.jnet.common.buffer2.HeapChunk;
import com.jfireframework.jnet.common.buffer2.HeapChunkList;
import com.jfireframework.jnet.common.buffer2.PooledBuffer;
import com.jfireframework.jnet.common.buffer2.PooledDirectBuffer;
import com.jfireframework.jnet.common.buffer2.PooledHeapBuffer;

@RunWith(Parameterized.class)
public class PoolChunkListTest
{
	private ChunkList<?>	poolChunkList;
	private ChunkList<?>	next;
	private ChunkList<?>	pre;
	private PooledBuffer<?>	pooledBuffer;
	private static Field	headField;
	static Field			parentField;
	static
	{
		try
		{
			headField = ChunkList.class.getDeclaredField("head");
			headField.setAccessible(true);
			parentField = Chunk.class.getDeclaredField("parent");
			parentField.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e)
		{
		}
	}
	
	/**
	 * 受测试的PoolChunkList的使用率在25-50之间
	 * 
	 * @param poolChunkList
	 * @param chunk
	 * @param pooledBuffer
	 */
	public <T> PoolChunkListTest(ChunkList<T> poolChunkList, ChunkList<T> next, ChunkList<T> pre, Chunk<T> chunk, PooledBuffer<T> pooledBuffer)
	{
		this.poolChunkList = poolChunkList;
		this.next = next;
		this.pre = pre;
		this.pooledBuffer = pooledBuffer;
		poolChunkList.addFromPrev(chunk, 0);
	}
	
	@Parameters
	public static Collection<?> param()
	{
		ChunkList<byte[]> top = new HeapChunkList(75, Integer.MAX_VALUE, null, 16);
		ChunkList<byte[]> poolChunkList = new HeapChunkList(25, 50, top, 16);
		ChunkList<byte[]> pre = new HeapChunkList(0, 40, poolChunkList, 16);
		ChunkList<ByteBuffer> top1 = new DirectChunkList(75, Integer.MAX_VALUE, null, 16);
		ChunkList<ByteBuffer> poolChunkList1 = new DirectChunkList(25, 50, top1, 16);
		ChunkList<ByteBuffer> pre1 = new DirectChunkList(0, 40, poolChunkList1, 16);
		poolChunkList.setPrevList(pre);
		return Arrays.asList(new Object[][] { //
		        { poolChunkList, top, pre, new HeapChunk(4, 1), new PooledHeapBuffer() }, //
		        { poolChunkList1, top1, pre1, new DirectChunk(4, 1), new PooledDirectBuffer() },//
		
		});
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public <T> void test() throws IllegalArgumentException, IllegalAccessException
	{
		Chunk<T> head = (Chunk<T>) headField.get(poolChunkList);
		assertTrue(parentField.get(head) == poolChunkList);
		int req = 12;
		boolean allocate = ((ChunkList<T>) poolChunkList).allocate(req, req, (PooledBuffer<T>) pooledBuffer);
		assertFalse(allocate);
		for (int i = 0; i < 8; i++)
		{
			allocate = ((ChunkList<T>) poolChunkList).allocate(1, 1, (PooledBuffer<T>) pooledBuffer);
			assertTrue(allocate);
		}
		allocate = ((ChunkList<T>) poolChunkList).allocate(1, 1, (PooledBuffer<T>) pooledBuffer);
		assertTrue(allocate);
		// 分配成功后head为空。因为节点被移动
		assertNull(headField.get(poolChunkList));
		assertTrue(headField.get(next) == head);
		assertTrue(parentField.get(head) == next);
	}
}
