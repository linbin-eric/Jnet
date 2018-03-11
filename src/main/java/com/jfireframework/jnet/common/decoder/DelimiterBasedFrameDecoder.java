package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.exception.TooLongException;
import com.jfireframework.jnet.common.util.Allocator;
import com.jfireframework.pool.ioBuffer.IoBuffer;

/**
 * 特定结束符整包解码器
 * 
 * @author 林斌
 * 
 */
public class DelimiterBasedFrameDecoder implements ReadProcessor<IoBuffer>
{
    private byte[] delimiter;
    private int    maxLength;
    
    /**
     * 
     * @param delimiter 解码使用的特定字节数组
     * @param maxLength 读取的码流最大长度。超过这个长度还未发现结尾分割字节数组，就会抛出异常
     */
    public DelimiterBasedFrameDecoder(byte[] delimiter, int maxLength)
    {
        this.maxLength = maxLength;
        this.delimiter = delimiter;
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
            if (ioBuf.remainRead() > maxLength)
            {
                throw new TooLongException();
            }
            ioBuf.maskRead();
            int index = ioBuf.indexOf(delimiter);
            if (index == -1)
            {
                ioBuf.compact().expansion(ioBuf.size() * 2);
                return;
            }
            else
            {
                int contentLength = index - ioBuf.readPosi();
                IoBuffer buf = Allocator.allocateDirect(contentLength);
                buf.put(ioBuf, contentLength);
                ioBuf.readPosi(index + delimiter.length);
                chain.chain(buf);
            }
        } while (true);
    }
    
}
