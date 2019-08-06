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

    void bindUpStream(DataProcessor<?> upStream);

    /**
     * 处理由上一个Invoker传递过来的数据。如果自身处理不了或者下游处理不了，均返回false
     *
     * @param data
     * @throws Throwable
     */
    boolean process(T data) throws Throwable;

    /**
     * 返回是否可以接受上游传递数据
     *
     * @return
     */
    boolean canAccept();

    /**
     * 写处理器通知当前发送队列有可供写出的容量。
     */
    void notifyedWriterAvailable() throws Throwable;

    /**
     * 当前对象是否能存储数据，如果可以，则在调用链路中其之前均需要放置BackPressureHelper。
     *
     * @return
     */
    boolean catStoreData();
}
