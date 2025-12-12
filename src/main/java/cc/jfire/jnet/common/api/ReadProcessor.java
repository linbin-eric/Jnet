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
     * 首次读取注册之前触发
     */
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        next.firePipelineComplete(pipeline);
    }
}
