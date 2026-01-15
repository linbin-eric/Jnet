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
     * 单次读取完毕后触发的方法。
     * Pipeline默认提供的 tailReadProcessor，对该方法的实现是注册下一次的读取。
     * 因此如果没有特别的需要，不要重写该方法。
     */
    default void readCompleted(ReadProcessorNode next)
    {
        next.fireReadCompleted();
    }

    /**
     * 首次读取注册之前触发。
     * 但是要注意，由于该事件需要靠前一个节点向后传递，因此也可能出现前一个节点拦截该事件的可能。
     */
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        next.firePipelineComplete(pipeline);
    }
}
