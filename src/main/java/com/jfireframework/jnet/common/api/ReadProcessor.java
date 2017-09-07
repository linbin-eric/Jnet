package com.jfireframework.jnet.common.api;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;

public interface ReadProcessor
{
	void process(ByteBuf<?> buf, ChannelContext channelContext) throws Throwable;
}
