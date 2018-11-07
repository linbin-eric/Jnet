package com.jfireframework.jnet.common.recycler;

import com.jfireframework.jnet.common.thread.FastThreadLocal;
import com.jfireframework.jnet.common.thread.FastThreadLocalThread;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class FastThreadLocalTest
{
    private static final String                  instance = "123";
    volatile             boolean                 fail     = false;
    private              FastThreadLocal<String> local    = new FastThreadLocal<String>()
    {
        @Override
        protected String initializeValue()
        {
            return instance;
        }
    };

    @Test
    public void test()
    {
        assertTrue(local.get() == instance);
        local.set("12sder");
        assertEquals("12sder", local.get());
        local.remove();
        assertTrue(local.get() == instance);
    }

    /**
     * 验证在FastThreadLocalThread中的效果
     *
     * @throws InterruptedException
     */
    @Test
    public void test2() throws InterruptedException
    {
        final CountDownLatch latch = new CountDownLatch(1);
        new FastThreadLocalThread(new Runnable()
        {

            @Override
            public void run()
            {
                try
                {
                    assertTrue(local.get() == instance);
                    assertTrue(local.get() == instance);
                } catch (Exception e)
                {
                    fail = true;
                } finally
                {
                    latch.countDown();
                }
            }
        }).start();
        latch.await();
        assertFalse(fail);
    }
}
