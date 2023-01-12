package com.jfirer.jnet.common.recycler;

public interface RecycleHandler<T>
{
    /**
     * 对value对象进行缓存回收
     *
     * @param value
     */
    void recycle(T value);
}