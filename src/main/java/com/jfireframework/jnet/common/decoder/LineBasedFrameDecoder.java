package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.exception.TooLongException;
import com.jfireframework.jnet.common.mem.buffer.IoBuffer;
import com.jfireframework.jnet.common.util.Allocator;

public class LineBasedFrameDecoder implements ReadProcessor<IoBuffer>
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
    public void process(IoBuffer ioBuffer, ProcessorChain chain, ChannelContext channelContext) throws Throwable
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
                    ioBuffer.compact().expansion(ioBuffer.size() * 2);
                    return;
                }
            }
            else
            {
                int length;
                if ('\r' == ioBuffer.get(eol - 1))
                {
                    length = eol - ioBuffer.readPosi() - 1;
                }
                else
                {
                    length = eol - ioBuffer.readPosi();
                }
                IoBuffer frame = Allocator.allocateDirect(length);
                frame.put(ioBuffer, length);
                ioBuffer.readPosi(eol + 1);
                chain.chain(frame);
            }
        } while (true);
    }
    
    private int getEndOfLine(IoBuffer byteBuf)
    {
        final int readIndex = byteBuf.readPosi();
        final int writeIndex = byteBuf.writePosi();
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
