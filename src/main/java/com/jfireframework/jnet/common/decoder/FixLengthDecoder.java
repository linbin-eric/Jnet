package com.jfireframework.jnet.common.decoder;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;

public class FixLengthDecoder implements ReadProcessor
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
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        ByteBuf<?> ioBuf = (ByteBuf<?>) data;
        do
        {
            
            if (ioBuf.remainRead() < frameLength)
            {
            	ioBuf.compact();
                return;
            }
            ByteBuf<?> buf = DirectByteBuf.allocate(frameLength);
            buf.put(ioBuf, frameLength);
            ioBuf.addReadIndex(frameLength);
            chain.chain(ioBuf);
        } while (true);
    }
    
}
