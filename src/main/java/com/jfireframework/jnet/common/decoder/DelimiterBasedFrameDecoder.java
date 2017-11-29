package com.jfireframework.jnet.common.decoder;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.exception.TooLongException;

/**
 * 特定结束符整包解码器
 * 
 * @author 林斌
 * 
 */
public class DelimiterBasedFrameDecoder implements ReadProcessor
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
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        ByteBuf<?> ioBuffer = (ByteBuf<?>) data;
        do
        {
            if (ioBuffer.remainRead() > maxLength)
            {
                throw new TooLongException();
            }
            ioBuffer.maskRead();
            int index = ioBuffer.indexOf(delimiter);
            if (index == -1)
            {
                return;
            }
            else
            {
                int contentLength = index - ioBuffer.readIndex();
                DirectByteBuf buf = DirectByteBuf.allocate(contentLength);
                buf.put(ioBuffer, contentLength);
                ioBuffer.readIndex(index + delimiter.length);
                chain.chain(buf);
            }
        } while (true);
    }
    
}
