package cc.jfire.jnet.common.api;

public interface WriteProcessorNode
{
    void fireWrite(Object data);

    void fireChannelClosed(Throwable e);

    WriteProcessorNode getNext();

    void setNext(WriteProcessorNode next);

    Pipeline pipeline();
}
