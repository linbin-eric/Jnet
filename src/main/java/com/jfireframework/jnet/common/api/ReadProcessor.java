package com.jfireframework.jnet.common.api;

public interface ReadProcessor<T>
{
    /**
     * 通道初始化时被调用
     * 
     * @param channelContext
     */
    void initialize(ChannelContext channelContext);
    
    /**
     * 处理器数据
     * 
     * @param data
     * @param chain
     */
    void process(T data, ProcessorChain chain, ChannelContext channelContext);
}
