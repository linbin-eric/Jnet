package com.jfireframework.jnet.common.api;

public interface DataProcessor<T>
{
    /**
     * 通道初始化时被调用
     *
     * @param channelContext
     */
    void bind(ChannelContext channelContext);

    void bindDownStream(DataProcessor<?> downStream);

    /**
     * 处理由上一个Invoker传递过来的数据。如果自身处理不了或者下游处理不了，均返回false
     *
     * @param data
     * @throws Throwable
     */
    void process(T data) throws Throwable;
}
