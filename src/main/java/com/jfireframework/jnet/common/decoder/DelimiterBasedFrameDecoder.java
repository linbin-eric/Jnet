package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;

/**
 * 特定结束符整包解码器
 * 
 * @author 林斌
 * 
 */
public class DelimiterBasedFrameDecoder implements DataProcessor<IoBuffer>
{
    private byte[]          delimiter;
    private int             maxLength;
    private BufferAllocator allocator;
    
    /**
     * 
     * @param delimiter 解码使用的特定字节数组
     * @param maxLength 读取的码流最大长度。超过这个长度还未发现结尾分割字节数组，就会抛出异常
     */
    public DelimiterBasedFrameDecoder(byte[] delimiter, int maxLength, BufferAllocator allocator)
    {
        this.maxLength = maxLength;
        this.delimiter = delimiter;
        this.allocator = allocator;
    }
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(IoBuffer ioBuf, ProcessorInvoker next) throws Throwable
    {
        do
        {
            if (ioBuf.remainRead() > maxLength)
            {
                throw new TooLongException();
            }
            int index = ioBuf.indexOf(delimiter);
            if (index == -1)
            {
                if (ioBuf.getReadPosi() == 0)
                {
                    ioBuf.capacityReadyFor(ioBuf.capacity() * 2);
                }
                else
                {
                    ioBuf.compact();
                }
                return;
            }
            else
            {
                int contentLength = index - ioBuf.getReadPosi();
                IoBuffer packet = allocator.ioBuffer(contentLength);
                packet.put(ioBuf, contentLength);
                ioBuf.setReadPosi(index + delimiter.length);
                next.process(packet);
            }
        } while (true);
    }
    
    @Override
    public boolean backpressureProcess(IoBuffer ioBuf, ProcessorInvoker next) throws Throwable
    {
        do
        {
            if (ioBuf.remainRead() > maxLength)
            {
                throw new TooLongException();
            }
            int index = ioBuf.indexOf(delimiter);
            if (index == -1)
            {
                if (ioBuf.getReadPosi() == 0)
                {
                    ioBuf.capacityReadyFor(ioBuf.capacity() * 2);
                }
                else
                {
                    ioBuf.compact();
                }
                return true;
            }
            else
            {
                int contentLength = index - ioBuf.getReadPosi();
                IoBuffer packet = allocator.ioBuffer(contentLength);
                packet.put(ioBuf, contentLength);
                ioBuf.setReadPosi(index + delimiter.length);
                boolean process = next.backpressureProcess(packet);
                if (process == false)
                {
                    return false;
                }
            }
        } while (true);
    }
    
}
