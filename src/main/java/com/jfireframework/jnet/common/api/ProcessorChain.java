package com.jfireframework.jnet.common.api;

public interface ProcessorChain
{
    
    /**
     * 传递参数到下一个节点处理器
     * 
     * @param data
     */
    void chain(Object data);
}
