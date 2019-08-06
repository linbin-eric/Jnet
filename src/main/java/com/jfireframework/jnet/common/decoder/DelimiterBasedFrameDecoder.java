package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;

/**
 * 特定结束符整包解码器
 *
 * @author 林斌
 */
public class DelimiterBasedFrameDecoder extends BindDownAndUpStreamDataProcessor<IoBuffer>
{
    private byte[]          delimiter;
    private int             maxLength;
    private BufferAllocator allocator;

    /**
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
    public boolean process(IoBuffer ioBuf) throws Throwable
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
                int      contentLength = index - ioBuf.getReadPosi();
                IoBuffer packet        = allocator.ioBuffer(contentLength);
                packet.put(ioBuf, contentLength);
                ioBuf.setReadPosi(index + delimiter.length);
                if (downStream.process(packet) == false)
                {
                    return false;
                }
            }
        } while (true);
    }

    @Override
    public void notifyedWriterAvailable() throws Throwable
    {
        upStream.notifyedWriterAvailable();
    }

    @Override
    public boolean catStoreData()
    {
        return false;
    }
}
