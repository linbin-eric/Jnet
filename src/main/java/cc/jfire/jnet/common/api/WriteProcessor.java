package cc.jfire.jnet.common.api;

public interface WriteProcessor<T>
{
    default void write(T data, WriteProcessorNode next)
    {
        next.fireWrite(data);
    }

    default void channelClosed(WriteProcessorNode next, Throwable e)
    {
        next.fireChannelClosed(e);
    }
}
