package com.jfireframework.jnet.common.channelcontext;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.businessprocessor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.readprocessor.ChannelAttachReadProcessor;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ChannelAttachChannelContext extends BaseChannelContext
{
    
    public ChannelAttachChannelContext(//
            ExecutorService businessExecutorService, //
            SendBufStorage bufStorage, //
            int maxMerge, //
            AioListener aioListener, //
            StreamProcessor[] inProcessors, //
            StreamProcessor[] outProcessors, //
            AsynchronousSocketChannel socketChannel, //
            FrameDecodec frameDecodec)
    {
        super(businessExecutorService, bufStorage, maxMerge, aioListener, inProcessors, outProcessors, socketChannel, frameDecodec);
    }
    
    @Override
    protected ReadProcessor buildReadProcessor(ExecutorService businessExecutorService, AioListener serverListener, ChannelContext channelContext, SendBufStorage bufStorage, WriteHandler writeHandler, StreamProcessor[] inProcessors)
    {
        ChannelAttachProcessor channelAttachProcessor = new ChannelAttachProcessor(businessExecutorService, serverListener, channelContext, bufStorage, writeHandler, inProcessors);
        return new ChannelAttachReadProcessor(channelAttachProcessor);
    }
    
}
