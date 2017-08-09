package com.jfireframework.jnet.common.readprocessor;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.businessprocessor.MutlisAttachProcessor;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class MutliAttachReadHandler implements ReadProcessor
{
    private final MutlisAttachProcessor processor;
    
    public MutliAttachReadHandler(MutlisAttachProcessor processor)
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
