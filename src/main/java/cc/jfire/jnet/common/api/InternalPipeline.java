package cc.jfire.jnet.common.api;

public interface InternalPipeline extends Pipeline
{
    void fireRead(Object data);

    void complete();

    void fireReadFailed(Throwable e);

    void fireWriteFailed(Throwable e);

}
