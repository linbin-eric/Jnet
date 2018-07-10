package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;
import com.jfireframework.jnet.common.util.Allocator;

public class LineBasedFrameDecoder implements DataProcessor<PooledIoBuffer>
{
    private int maxLineLength;
    
    /**
     * 换行符报文解码器。
     * 
     * @param maxLineLength 可读取的最大长度，超过最大长度还未读取到换行符，则抛出异常
     */
    public LineBasedFrameDecoder(int maxLineLength)
    {
        this.maxLineLength = maxLineLength;
    }
    
    @Override
    public void initialize(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    public void process(PooledIoBuffer ioBuffer, ProcessorChain chain, ChannelContext channelContext) throws Throwable
    {
        do
        {
            
            int eol = getEndOfLine(ioBuffer);
            if (eol == -1)
            {
                if (ioBuffer.remainRead() > maxLineLength)
                {
                    throw new TooLongException();
                }
                else
                {
                    ioBuffer.compact().grow(ioBuffer.capacity() * 2);
                    return;
                }
            }
            else
            {
                int length;
                if ('\r' == ioBuffer.get(eol - 1))
                {
                    length = eol - ioBuffer.getReadPosi() - 1;
                }
                else
                {
                    length = eol - ioBuffer.getReadPosi();
                }
                IoBuffer frame = Allocator.allocateDirect(length);
                frame.put(ioBuffer, length);
                ioBuffer.setReadPosi(eol + 1);
                chain.chain(frame);
            }
        } while (true);
    }
    
    private int getEndOfLine(IoBuffer byteBuf)
    {
        final int readIndex = byteBuf.getReadPosi();
        final int writeIndex = byteBuf.getWritePosi();
        for (int i = readIndex; i < writeIndex; i++)
        {
            byte b;
            try
            {
                b = byteBuf.get(i);
            }
            catch (Exception e)
            {
                System.err.println(readIndex);
                System.err.println(writeIndex);
                System.err.println(i);
                throw e;
            }
            if (b == '\n')
            {
                return i;
            }
        }
        return -1;
    }
    
}
