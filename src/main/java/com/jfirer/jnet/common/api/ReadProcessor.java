package com.jfirer.jnet.common.api;

import java.nio.channels.ClosedChannelException;
import java.nio.channels.ShutdownChannelGroupException;

public interface ReadProcessor<T>
{
    ReadProcessor TAIL = new ReadProcessor()
    {
        @Override
        public void read(Object data, ReadProcessorNode next)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void pipelineComplete(ReadProcessorNode next)
        {
        }

        @Override
        public void channelClose(ReadProcessorNode next, Throwable e)
        {
        }

        @Override
        public void exceptionCatch(Throwable e, ReadProcessorNode next)
        {
            if (e instanceof ClosedChannelException == false && e instanceof ShutdownChannelGroupException == false)
            {
                e.printStackTrace();
            }
        }

        @Override
        public void readClose(ReadProcessorNode next)
        {
        }
    };

    /**
     * 有数据被读取时触发
     *
     * @param data
     * @param next
     */
    void read(T data, ReadProcessorNode next);

    /**
     * 首次读取注册之前触发
     *
     * @param next
     */
    default void pipelineComplete(ReadProcessorNode next)
    {
        next.firePipelineComplete();
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
