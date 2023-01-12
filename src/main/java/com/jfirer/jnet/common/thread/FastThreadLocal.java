package com.jfirer.jnet.common.thread;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class FastThreadLocal<T>
{
    // 下标0用作特殊用途，暂时保留
    static final  AtomicInteger IDGENERATOR = new AtomicInteger(1);
    private final int           idx         = IDGENERATOR.getAndIncrement();
    private       Supplier<T>   supplier    = () -> null;

    public static <T> FastThreadLocal<T> withInitializeValue(Supplier<T> supplier)
    {
        FastThreadLocal<T> fastThreadLocal = new FastThreadLocal<>();
        fastThreadLocal.supplier = supplier;
        return fastThreadLocal;
    }

    @SuppressWarnings("unchecked")
    public T get()
    {
        FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.get();
        T                  result             = (T) fastThreadLocalMap.get(idx);
        if (result != null)
        {
            return result;
        }
        result = supplier.get();
        if (result == null)
        {
            return null;
        }
        else
        {
            fastThreadLocalMap.set(result, idx);
            return result;
        }
    }

    public void remove()
    {
        FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.getIfSet();
        if (fastThreadLocalMap != null)
        {
            fastThreadLocalMap.remove(idx);
        }
    }

    public void set(T value)
    {
        FastThreadLocalMap fastThreadLocalMap = FastThreadLocalMap.get();
        fastThreadLocalMap.set(value, idx);
    }
}
