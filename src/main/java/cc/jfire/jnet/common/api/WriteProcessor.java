package cc.jfire.jnet.common.api;

public interface WriteProcessor<T>
{
    default void write(T data, WriteProcessorNode next)
    {
        next.fireWrite(data);
    }

    default void writeFailed(WriteProcessorNode next, Throwable e)
    {
        next.fireWriteFailed(e);
    }
}
