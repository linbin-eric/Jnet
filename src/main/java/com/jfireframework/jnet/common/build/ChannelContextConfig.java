package com.jfireframework.jnet.common.build;

import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class ChannelContextConfig
{
    private StreamProcessor[] outProcessors;
    private StreamProcessor[] inProcessors;
    private SendBufStorage        bufStorage;
    private int               maxMerge = 10;
    private FrameDecodec      frameDecodec;
    
    public FrameDecodec getFrameDecodec()
    {
        return frameDecodec;
    }
    
    public void setFrameDecodec(FrameDecodec frameDecodec)
    {
        this.frameDecodec = frameDecodec;
    }
    
    public int getMaxMerge()
    {
        return maxMerge;
    }
    
    public void setMaxMerge(int maxMerge)
    {
        this.maxMerge = maxMerge;
    }
    
    public SendBufStorage getBufStorage()
    {
        return bufStorage;
    }
    
    public void setBufStorage(SendBufStorage bufStorage)
    {
        this.bufStorage = bufStorage;
    }
    
    public StreamProcessor[] getOutProcessors()
    {
        return outProcessors;
    }
    
    public void setOutProcessors(StreamProcessor... outProcessors)
    {
        this.outProcessors = outProcessors;
    }
    
    public StreamProcessor[] getInProcessors()
    {
        return inProcessors;
    }
    
    public void setInProcessors(StreamProcessor... inProcessors)
    {
        this.inProcessors = inProcessors;
    }
    
}
