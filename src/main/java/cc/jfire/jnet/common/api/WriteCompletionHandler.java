package cc.jfire.jnet.common.api;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, ByteBuffer>
{
    void write(IoBuffer buffer);
}
