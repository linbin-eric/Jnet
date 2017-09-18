package com.jfireframework.jnet.common.api;

import java.nio.channels.CompletionHandler;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;

public interface WriteHandler extends CompletionHandler<Integer, ByteBuf<?>>
{
	void registerWrite();
}
