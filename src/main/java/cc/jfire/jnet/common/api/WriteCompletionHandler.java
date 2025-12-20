package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

public interface WriteCompletionHandler
{
    void write(IoBuffer buffer);

    void noticeClose();

    int get();

    void setWriteListener(WriteListener writeListener);
}
