package com.jfireframework.jnet.common.decoder;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.exception.TooLongException;

public class LineBasedFrameDecoder implements ReadProcessor
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
    public void process(Object data, ProcessorChain chain, ChannelContext channelContext)
    {
        ByteBuf<?> ioBuffer = (ByteBuf<?>) data;
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
                	ioBuffer.compact();
                    return;
                }
            }
            else
            {
                int length;
                if ('\r' == ioBuffer.get(eol - 1))
                {
                    length = eol - ioBuffer.readIndex() - 1;
                }
                else
                {
                    length = eol - ioBuffer.readIndex();
                }
                DirectByteBuf frame = DirectByteBuf.allocate(length);
                frame.put(ioBuffer, length);
                ioBuffer.readIndex(eol + 1);
                chain.chain(frame);
            }
        } while (true);
    }
    
    private int getEndOfLine(ByteBuf<?> byteBuf)
    {
        final int readIndex = byteBuf.readIndex();
        final int writeIndex = byteBuf.writeIndex();
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
