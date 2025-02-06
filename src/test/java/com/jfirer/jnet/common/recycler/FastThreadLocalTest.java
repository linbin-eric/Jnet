package com.jfirer.jnet.common.recycler;

import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class FastThreadLocalTest
{
    private static final String                  instance = "123";
    volatile      boolean                 fail  = false;
    private final FastThreadLocal<String> local = FastThreadLocal.withInitializeValue(() -> instance);

    @Test
    public void test()
    {
        assertSame(instance, local.get());
        local.set("12sder");
        assertEquals("12sder", local.get());
        local.remove();
        assertSame(instance, local.get());
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
                    assertSame(instance, local.get());
                    assertSame(instance, local.get());
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
