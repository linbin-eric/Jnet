package com.jfireframework.jnet.common.recycler;

public interface RecycleHandler
{
    /**
     * 对value对象进行缓存回收
     *
     * @param value
     */
    void recycle(Object value);
}