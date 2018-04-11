package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import com.jfireframework.jnet.common.mem.archon.Archon;
import com.jfireframework.jnet.common.mem.archon.DirectPooledArchon;
import com.jfireframework.jnet.common.mem.archon.HeapPooledArchon;
import com.jfireframework.jnet.common.mem.handler.DirectIoBuffer;
import com.jfireframework.jnet.common.mem.handler.HeapIoBuffer;

public class HandlerTest
{
	/**
	 * heap扩容测试
	 */
	@Test
	public void test()
	{
		Archon archon = new HeapPooledArchon(4, 1);
		HeapIoBuffer handler = new HeapIoBuffer();
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
		Archon archon = new DirectPooledArchon(4, 1);
		DirectIoBuffer handler = new DirectIoBuffer();
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
}
