package com.jfirer.jnet.common.api;

import com.jfirer.jnet.common.buffer.IoBuffer;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

public interface WriteCompletionHandler extends CompletionHandler<Integer, ByteBuffer>
{
    void write(IoBuffer buffer);
}
