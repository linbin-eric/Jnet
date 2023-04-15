package com.jfirer.jnet.common.recycler;

import com.jfirer.jnet.common.thread.FastThreadLocal;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class RecycleTest
{
    Recycler<Entry> recycler;
    private Field recycleIdField;
    private Field lastRecycleIdField;
    private Field currentStackField;
    private Field sharedCapacityField;

    public RecycleTest()
    {
        try
        {
            recycleIdField = Recycler.DefaultHandler.class.getDeclaredField("recyclerId");
            recycleIdField.setAccessible(true);
            lastRecycleIdField = Recycler.DefaultHandler.class.getDeclaredField("lastRecycleId");
            lastRecycleIdField.setAccessible(true);
            currentStackField = Recycler.class.getDeclaredField("currentStack");
            currentStackField.setAccessible(true);
            sharedCapacityField = Recycler.Stack.class.getDeclaredField("sharedCapacity");
            sharedCapacityField.setAccessible(true);
        }
        catch (NoSuchFieldException | SecurityException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Before
    public void before()
    {
        recycler = new Recycler<>(Entry::new, (entry, handler) -> entry.handler = handler);
    }

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
        int         max     = Recycler.MAX_CACHE_INSTANCE_CAPACITY;
        List<Entry> list    = new LinkedList<>();
        Entry       another = recycler.get();
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
        Entry entry  = recycler.get();
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
        final Entry          entry = recycler.get();
        final CountDownLatch latch = new CountDownLatch(1);
        new FastThreadLocalThread(new Runnable()
        {
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
        entry2.handler.recycle(entry2);
        Entry entry3 = recycler.get();
        assertTrue(entry2 == entry3);
    }

    /**
     * 测试延迟队列最大接收数据量
     *
     * @throws InterruptedException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    @Test
    public void test5() throws InterruptedException, IllegalArgumentException, IllegalAccessException
    {
        int              num     = Recycler.MAX_SHARED_CAPACITY;
        final Entry      another = recycler.get();
        final Set<Entry> set     = new HashSet<>();
        for (int i = 0; i < num; i++)
        {
            set.add(recycler.get());
        }
        final CountDownLatch latch  = new CountDownLatch(1);
        final CountDownLatch latch2 = new CountDownLatch(1);
        new FastThreadLocalThread(new Runnable()
        {
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
                    latch.countDown();
                }
            }
        }).start();
        AtomicInteger shareCapacity = getShareCapacity();
        // 尚未执行其他线程暂存时共享大小还是最大值
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
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private AtomicInteger getShareCapacity() throws IllegalAccessException
    {
        FastThreadLocal<Recycler.Stack> object        = (FastThreadLocal<Recycler.Stack>) currentStackField.get(recycler);
        Recycler.Stack                  stack         = object.get();
        AtomicInteger                   shareCapacity = (AtomicInteger) sharedCapacityField.get(stack);
        return shareCapacity;
    }

    /**
     * 多个线程同时回收.
     *
     * @throws InterruptedException
     * @throws IllegalAccessException
     */
    @Test
    public void test6() throws InterruptedException, IllegalAccessException
    {
        int                num   = Recycler.MAX_SHARED_CAPACITY;
        final Queue<Entry> queue = new ConcurrentLinkedQueue<>();
        Set<Entry>         set   = new HashSet<>();
        for (int i = 0; i < num; i++)
        {
            Entry entry = recycler.get();
            set.add(entry);
            queue.add(entry);
        }
        // 由于最大尝试次数是3，因此线程数不能大于3。为了平分正确，因此设定为2
        int                  threadNum   = 2;
        final int            numPerThrad = num / threadNum;
        final CyclicBarrier  barrier     = new CyclicBarrier(threadNum);
        final CountDownLatch latch       = new CountDownLatch(threadNum);
        for (int i = 0; i < threadNum; i++)
        {
            new FastThreadLocalThread(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        barrier.await();
                    }
                    catch (InterruptedException | BrokenBarrierException e)
                    {
                        e.printStackTrace();
                    }
                    Entry entry;
                    int   size = 0;
                    while (size < numPerThrad && (entry = queue.poll()) != null)
                    {
                        entry.handler.recycle(entry);
                        size++;
                    }
                    latch.countDown();
                }
            }).start();
        }
        latch.await();
        assertTrue(queue.isEmpty());
        AtomicInteger shareCapacity = getShareCapacity();
        assertEquals(0, shareCapacity.get());
        for (int i = 0; i < num; i++)
        {
            set.remove(recycler.get());
        }
        assertEquals(0, set.size());
    }

    /**
     * 检查是否会归还剩余的共享容量
     *
     * @throws InterruptedException
     */
    @Test
    public void test7() throws InterruptedException
    {
        int                size  = Recycler.LINK_SIZE + (Recycler.LINK_SIZE >> 1);
        final Queue<Entry> queue = new LinkedList<>();
        for (int i = 0; i < size; i++)
        {
            queue.add(recycler.get());
        }
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (Entry each : queue)
                {
                    each.handler.recycle(each);
                }
                latch.countDown();
            }
        }).start();
        latch.await();
        Recycler<Entry>.Stack stack = recycler.currentStack.get();
        assertEquals(Recycler.MAX_SHARED_CAPACITY - 2 * Recycler.LINK_SIZE, stack.sharedCapacity.get());
        System.gc();
        Thread.sleep(100);
        System.gc();
        for (int i = 0; i < size; i++)
        {
            recycler.get();
            if (i == 0)
            {
                assertEquals(Recycler.MAX_SHARED_CAPACITY - Recycler.LINK_SIZE, stack.sharedCapacity.get());
            }
        }
        assertEquals(Recycler.MAX_SHARED_CAPACITY - Recycler.LINK_SIZE, stack.sharedCapacity.get());
        recycler.get();
        assertEquals(Recycler.MAX_SHARED_CAPACITY, stack.sharedCapacity.get());
    }

    /**
     * 测试归还剩余容量是否会导致溢出
     */
    @Test
    public void test8() throws InterruptedException
    {
        int                size  = Recycler.LINK_SIZE - 1;
        final Queue<Entry> queue = new LinkedList<>();
        for (int i = 0; i < size; i++)
        {
            queue.add(recycler.get());
        }
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(() -> {
            for (Entry each : queue)
            {
                each.handler.recycle(each);
            }
            latch.countDown();
        }).start();
        latch.await();
        Recycler<Entry>.Stack stack = recycler.currentStack.get();
        assertEquals(Recycler.MAX_SHARED_CAPACITY - Recycler.LINK_SIZE, stack.sharedCapacity.get());
        System.gc();
        Thread.sleep(100);
        System.gc();
        for (int i = 0; i < size; i++)
        {
            recycler.get();
            if (i == 0)
            {
                assertEquals(Recycler.MAX_SHARED_CAPACITY - Recycler.LINK_SIZE, stack.sharedCapacity.get());
            }
        }
        assertEquals(Recycler.MAX_SHARED_CAPACITY - Recycler.LINK_SIZE, stack.sharedCapacity.get());
        recycler.get();
        assertEquals(Recycler.MAX_SHARED_CAPACITY, stack.sharedCapacity.get());
    }

    class Entry
    {
        RecycleHandler<Entry> handler;
        String                value;
    }
}
