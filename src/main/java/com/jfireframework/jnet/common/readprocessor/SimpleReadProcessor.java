package com.jfireframework.jnet.common.readprocessor;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class SimpleReadProcessor implements ReadProcessor
{
    
    @Override
    public void process(ByteBuf<?> buf, BufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
    {
        Object finalResult = ProcesserUtil.process(channelContext, inProcessors, buf, 0);
        if (finalResult instanceof ByteBuf<?>)
        {
            bufStorage.putBuf(buf);
            channelContext.registerWrite();
        }
    }
}
