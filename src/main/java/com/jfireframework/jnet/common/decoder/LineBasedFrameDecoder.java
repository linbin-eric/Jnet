package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;

public class LineBasedFrameDecoder implements DataProcessor<IoBuffer>
{
    private int             maxLineLength;
    private BufferAllocator allocator;
    
    /**
     * 换行符报文解码器。
     * 
     * @param maxLineLength 可读取的最大长度，超过最大长度还未读取到换行符，则抛出异常
     */
    public LineBasedFrameDecoder(int maxLineLength, BufferAllocator allocator)
    {
        this.maxLineLength = maxLineLength;
        this.allocator = allocator;
    }
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        // TODO Auto-generated method stub
        
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
    
    @Override
    public void process(IoBuffer ioBuffer, ProcessorInvoker next) throws Throwable
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
                    ioBuffer.compact().capacityReadyFor(ioBuffer.capacity() * 2);
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
                IoBuffer packet = allocator.ioBuffer(length);
                packet.put(ioBuffer, length);
                ioBuffer.setReadPosi(eol + 1);
                next.process(packet);
            }
        } while (true);
    }
    
    @Override
    public boolean backpressureProcess(IoBuffer ioBuffer, ProcessorInvoker next) throws Throwable
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
                    ioBuffer.compact().capacityReadyFor(ioBuffer.capacity() * 2);
                    return true;
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
                IoBuffer packet = allocator.ioBuffer(length);
                packet.put(ioBuffer, length);
                ioBuffer.setReadPosi(eol + 1);
                boolean process = next.backPressureProcess(packet);
                if (process == false)
                {
                    return process;
                }
            }
        } while (true);
    }
    
}
