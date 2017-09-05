package com.jfireframework.jnet.common.api;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public interface ReadProcessor
{
    void process(ByteBuf<?> buf, SendBufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable;
}
