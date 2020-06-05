package com.jfirer.jnet.common.api;

public interface ReadProcessor<T>
{
    /**
     * 有数据被读取时触发
     * @param data
     * @param next
     */
    void read(T data, ProcessorContext next);

    /**
     * 首次读取注册之前触发
     * @param next
     */
    default void prepareFirstRead(ProcessorContext next)
    {
        next.firePrepareFirstRead();
    }

    /**
     * 通道关闭时触发动作
     * @param next
     */
    default void channelClose(ProcessorContext next)
    {
        next.fireChannelClose();
    }

    /**
     * 异常发生时触发
     * @param e
     * @param next
     */
    default void exceptionCatch(Throwable e, ProcessorContext next)
    {
        next.fireExceptionCatch(e);
    }

    /**
     * 读取生命周期结束，后续不会再有读取相关动作产生。
     * @param next
     */
    default void endOfReadLife(ProcessorContext next)
    {
        next.fireEndOfReadLife();
    }
}
