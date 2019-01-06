package com.jfireframework.jnet.common.api;

public interface BoundaryBehavior
{
    /**
     * 当前对象是否具备有界特性。如果具备，则除了ReadCompletionHandler接口外，其余实现之前均需要放置BackPressureHelper实例
     * @return
     */
    boolean isBoundary();
}
