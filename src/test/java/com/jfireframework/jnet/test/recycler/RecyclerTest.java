package com.jfireframework.jnet.test.recycler;

import static org.junit.Assert.assertTrue;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import com.jfireframework.jnet.common.recycler.Recycler;
import com.jfireframework.jnet.common.recycler.Recycler.RecycleHandler;

public class RecyclerTest
{
	class Person
	{
		RecycleHandler recycleHandler;
	}
	
	private Recycler<Person> recycler = new Recycler<RecyclerTest.Person>() {
		
		@Override
		protected Person newObject(RecycleHandler handler)
		{
			Person person = new Person();
			person.recycleHandler = handler;
			return person;
		}
	};
	
	@Test
	public void testSingleThread()
	{
		Person person1 = recycler.get();
		Person person2 = recycler.get();
		assertTrue(person1 != person2);
		person2.recycleHandler.recycle(person2);
		Person person3 = recycler.get();
		assertTrue(person3 == person2);
		Person person4 = recycler.get();
		assertTrue(person4 != person3);
	}
	
	@Test
	public void testMutliThread() throws InterruptedException
	{
		final Person person1 = recycler.get();
		final CountDownLatch latch = new CountDownLatch(1);
		new Thread(new Runnable() {
			
			@Override
			public void run()
			{
				person1.recycleHandler.recycle(person1);
				latch.countDown();
			}
		}).start();
		latch.await();
		Person person2 = recycler.get();
		assertTrue(person2 == person1);
	}
}
