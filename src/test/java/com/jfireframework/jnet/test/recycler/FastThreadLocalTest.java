package com.jfireframework.jnet.test.recycler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import java.util.concurrent.CountDownLatch;
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
    boolean                         fail  = false;
    
    @Test
    public void test() throws InterruptedException
    {
        final CountDownLatch latch = new CountDownLatch(1);
        new FastThreadLocalThread(new Runnable() {
            
            @Override
            public void run()
            {
                try
                {
                    assertEquals("123", local.get());
                    assertEquals("123", local.get());
                }
                catch (Exception e)
                {
                    fail = true;
                }
                finally
                {
                    latch.countDown();
                }
            }
        }).start();
        latch.await();
        assertFalse(fail);
    }
}
