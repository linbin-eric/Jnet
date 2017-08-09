package com.jfireframework.jnet.common.api;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public interface ReadProcessor
{
    void process(ByteBuf<?> buf, BufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable;
}
