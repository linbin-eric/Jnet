package com.jfirer.jnet.common.api;

public interface Pipeline
{
    void fireWrite(Object data);

    void addReadProcessor(ReadProcessor<?> processor);

    void addReadProcessor(ReadProcessor<?> processor, WorkerGroup group);

    void addWriteProcessor(WriteProcessor<?> processor);

    void addWriteProcessor(WriteProcessor<?> processor, WorkerGroup group);

    ChannelContext channelContext();

    static void invokeMethodIgnoreException(Runnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable e)
        {
            ;
        }
    }
}
