package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.Allocator;

public class FixLengthDecoder implements ReadProcessor<PooledIoBuffer>
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
    public void process(PooledIoBuffer ioBuf, ProcessorChain chain, ChannelContext channelContext) throws Throwable
    {
        do
        {
            
            if (ioBuf.remainRead() < frameLength)
            {
                ioBuf.compact().grow(frameLength);
                return;
            }
            IoBuffer buf = Allocator.allocateDirect(frameLength);
            buf.put(ioBuf, frameLength);
            ioBuf.addReadPosi(frameLength);
            chain.chain(ioBuf);
        } while (true);
    }
    
}
