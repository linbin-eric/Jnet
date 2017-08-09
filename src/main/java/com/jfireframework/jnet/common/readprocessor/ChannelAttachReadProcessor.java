package com.jfireframework.jnet.common.readprocessor;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.businessprocessor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ChannelAttachReadProcessor implements ReadProcessor
{
    private final ChannelAttachProcessor processor;
    
    public ChannelAttachReadProcessor(ChannelAttachProcessor processor)
    {
        this.processor = processor;
    }
    
    @Override
    public void process(ByteBuf<?> buf, BufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
    {
        ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
        processor.commit(task);
    }
    
}
