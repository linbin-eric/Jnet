package com.jfireframework.jnet.test.recycler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import com.jfireframework.jnet.common.recycler.Recycler;
import com.jfireframework.jnet.common.recycler.Recycler.DefaultHandler;
import com.jfireframework.jnet.common.recycler.Recycler.RecycleHandler;
import com.jfireframework.jnet.common.recycler.Recycler.Stack;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;

public class RecycleTest
{
	
	class Entry
	{
		RecycleHandler	handler;
		String			value;
	}
	
	private Field	recycleIdField;
	private Field	lastRecycleIdField;
	private Field	currentStackField;
	private Field	sharedCapacityField;
	
	public RecycleTest()
	{
		try
		{
			recycleIdField = DefaultHandler.class.getDeclaredField("recyclerId");
			recycleIdField.setAccessible(true);
			lastRecycleIdField = DefaultHandler.class.getDeclaredField("lastRecycleId");
			lastRecycleIdField.setAccessible(true);
			currentStackField = Recycler.class.getDeclaredField("currentStack");
			currentStackField.setAccessible(true);
			sharedCapacityField = Stack.class.getDeclaredField("sharedCapacity");
			sharedCapacityField.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	Recycler<Entry> recycler = new Recycler<Entry>() {
		
		@Override
		protected Entry newObject(RecycleHandler handler)
		{
			Entry entry = new Entry();
			entry.handler = handler;
			return entry;
		}
	};
	
	@Test
	public void test()
	{
		Entry entry = recycler.get();
		assertNull(entry.value);
		entry.value = "123";
		entry.handler.recycle(entry);
		Entry entry2 = recycler.get();
		assertTrue(entry == entry2);
		assertEquals("123", entry2.value);
	}
	
	/**
	 * 检查回收到达极限会如何
	 * 
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@Test
	public void test2() throws IllegalArgumentException, IllegalAccessException
	{
		int max = Recycler.MAX_CACHE_INSTANCE_CAPACITY;
		List<Entry> list = new LinkedList<>();
		Entry another = recycler.get();
		for (int i = 0; i < max; i++)
		{
			list.add(recycler.get());
		}
		for (Entry each : list)
		{
			assertEquals(0, recycleIdField.getInt(each.handler));
			assertEquals(0, lastRecycleIdField.getInt(each.handler));
			each.handler.recycle(each);
			assertEquals(Recycler.recyclerId, recycleIdField.getInt(each.handler));
			assertEquals(Recycler.recyclerId, lastRecycleIdField.getInt(each.handler));
		}
		assertEquals(0, recycleIdField.getInt(another.handler));
		assertEquals(0, lastRecycleIdField.getInt(another.handler));
		another.handler.recycle(another);
		assertEquals(0, recycleIdField.getInt(another.handler));
		assertEquals(0, lastRecycleIdField.getInt(another.handler));
	}
	
	/**
	 * 测试当前线程重复回收
	 */
	@Test
	public void test3()
	{
		Entry entry = recycler.get();
		Entry entry2 = recycler.get();
		entry.handler.recycle(entry);
		try
		{
			entry2.handler.recycle(entry2);
		}
		catch (Exception e)
		{
			assertTrue(e instanceof IllegalStateException);
		}
	}
	
	/**
	 * 测试其他线程回收
	 * 
	 * @throws InterruptedException
	 */
	@Test
	public void test4() throws InterruptedException
	{
		final Entry entry = recycler.get();
		final CountDownLatch latch = new CountDownLatch(1);
		new FastThreadLocalThread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					entry.handler.recycle(entry);
				}
				finally
				{
					latch.countDown();
				}
			}
		}).start();
		latch.await();
		Entry entry2 = recycler.get();
		assertTrue(entry == entry2);
	}
	
	/**
	 * 测试延迟队列最大接收数据量
	 * 
	 * @throws InterruptedException
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void test5() throws InterruptedException, IllegalArgumentException, IllegalAccessException
	{
		int num = Recycler.MAX_SHARED_CAPACITY;
		final Entry another = recycler.get();
		final Set<Entry> set = new HashSet<>();
		for (int i = 0; i < num; i++)
		{
			set.add(recycler.get());
		}
		final CountDownLatch latch = new CountDownLatch(1);
		final CountDownLatch latch2 = new CountDownLatch(1);
		new FastThreadLocalThread(new Runnable() {
			
			@Override
			public void run()
			{
				try
				{
					latch2.await();
					for (Entry entry : set)
					{
						entry.handler.recycle(entry);
					}
					another.handler.recycle(another);
				}
				catch (IllegalArgumentException | InterruptedException e)
				{
					e.printStackTrace();
				}
				finally
				{
					System.out.println("输出");
					latch.countDown();
				}
			}
		}).start();
		FastThreadLocal<Stack> object = (FastThreadLocal<Stack>) currentStackField.get(recycler);
		Stack stack = object.get();
		AtomicInteger shareCapacity = (AtomicInteger) sharedCapacityField.get(stack);
		assertEquals(Recycler.MAX_SHARED_CAPACITY, shareCapacity.get());
		latch2.countDown();
		latch.await();
		assertEquals(0, shareCapacity.get());
		for (int i = 0; i < num; i++)
		{
			if (set.remove(recycler.get()) == false)
			{
				fail();
			}
		}
		assertTrue(another != recycler.get());
		System.out.println("ss");
	}
}
