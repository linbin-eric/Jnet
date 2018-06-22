package com.jfireframework.jnet.test.recycler;

import static org.junit.Assert.assertEquals;
import java.util.concurrent.locks.LockSupport;
import org.junit.Test;
import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;

public class FastThreadLocalTest
{
	private FastThreadLocal<String> local = new FastThreadLocal<String>() {
		@Override
		protected String initializeValue()
		{
			return "123";
		}
	};
	
	@Test
	public void test()
	{
		new FastThreadLocalThread(new Runnable() {
			
			@Override
			public void run()
			{
				assertEquals("123", local.get());
				assertEquals("123", local.get());
			}
		}).start();
		LockSupport.parkNanos(10000000000l);
	}
}
