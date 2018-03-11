package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.util.Allocator;
import com.jfireframework.pool.ioBuffer.IoBuffer;

public class FixLengthDecoder implements ReadProcessor<IoBuffer>
{
    private final int frameLength;
    
    /**
     * 固定长度解码器
     * 
     * @param frameLength 一个报文的固定长度
     */
    public FixLengthDecoder(int frameLength)
    {
        this.frameLength = frameLength;
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(IoBuffer ioBuf, ProcessorChain chain, ChannelContext channelContext) throws Throwable
    {
        do
        {
            
            if (ioBuf.remainRead() < frameLength)
            {
                ioBuf.compact().expansion(frameLength);
                return;
            }
            IoBuffer buf = Allocator.allocateDirect(frameLength);
            buf.put(ioBuf, frameLength);
            ioBuf.addReadPosi(frameLength);
            chain.chain(ioBuf);
        } while (true);
    }
    
}
