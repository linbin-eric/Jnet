package cc.jfire.jnet.common.api;

public interface ReadProcessor<T>
{
    /**
     * 有数据被读取时触发
     *
     * @param data
     * @param next
     */
    void read(T data, ReadProcessorNode next);

    default void readFailed(Throwable e, ReadProcessorNode next) {next.fireReadFailed(e);}

    /**
     * 本轮读取处理完成时触发，用于通知处理器本轮数据已全部处理完毕
     */
    default void readCompleted(ReadProcessorNode next)
    {
        next.fireReadCompleted();
    }

    /**
     * 首次读取注册之前触发
     */
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        next.firePipelineComplete(pipeline);
    }
}
