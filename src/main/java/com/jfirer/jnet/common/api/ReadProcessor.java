package com.jfirer.jnet.common.api;

public interface ReadProcessor<T>
{
    /**
     * 有数据被读取时触发
     *
     * @param data
     * @param next
     */
    void read(T data, ReadProcessorNode next);

    /**
     * 首次读取注册之前触发
     */
    default void pipelineComplete(Pipeline pipeline)
    {
        ;
    }

    /**
     * 通道关闭时触发动作
     *
     * @param next
     */
    default void channelClose(ReadProcessorNode next, Throwable e)
    {
        next.fireChannelClose(e);
    }

    /**
     * 异常发生时触发
     *
     * @param e
     * @param next
     */
    default void exceptionCatch(Throwable e, ReadProcessorNode next)
    {
        next.fireExceptionCatch(e);
    }

    /**
     * 读取生命周期结束，后续不会再有读取相关动作产生。
     *
     * @param next
     */
    default void readClose(ReadProcessorNode next)
    {
        next.fireReadClose();
    }
}
