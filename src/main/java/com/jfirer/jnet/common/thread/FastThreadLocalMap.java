package com.jfirer.jnet.common.thread;

import java.util.Arrays;

public class FastThreadLocalMap
{
    private static final ThreadLocal<FastThreadLocalMap> slowThreadLocal = new ThreadLocal<>();
    private              Object[]                        array           = new Object[16];

    public static FastThreadLocalMap getIfSet()
    {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof FastThreadLocalThread)
        {
            return ((FastThreadLocalThread) currentThread).getIfHaveFastThreadLocalMap();
        }
        else
        {
            return slowThreadLocal.get();
        }
    }

    public static FastThreadLocalMap get()
    {
        Thread currentThread = Thread.currentThread();
        if (currentThread instanceof FastThreadLocalThread)
        {
            return ((FastThreadLocalThread) currentThread).getOrInitializeFastThreadLocalMap();
        }
        else
        {
            FastThreadLocalMap fastThreadLocalMap = slowThreadLocal.get();
            if (fastThreadLocalMap == null)
            {
                fastThreadLocalMap = new FastThreadLocalMap();
                slowThreadLocal.set(fastThreadLocalMap);
            }
            return fastThreadLocalMap;
        }
    }

    public Object get(int idx)
    {
        return idx >= array.length ? null : array[idx];
    }

    public void remove(int idx)
    {
        if (idx >= array.length)
        {
            return;
        }
        array[idx] = null;
    }

    public void set(Object value, int idx)
    {
        if (idx >= array.length)
        {
            newArray(idx);
        }
        array[idx] = value;
    }

    private void newArray(int idx)
    {
        int length = array.length;
        while (idx >= length)
        {
            length += 8;
        }
        array = Arrays.copyOf(array, length);
    }
}
