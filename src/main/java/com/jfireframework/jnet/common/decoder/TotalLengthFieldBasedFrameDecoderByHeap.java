package com.jfireframework.jnet.common.decoder;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.api.ProcessorInvoker;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.exception.TooLongException;

public class TotalLengthFieldBasedFrameDecoderByHeap implements DataProcessor<IoBuffer>
{
    // 代表长度字段开始读取的位置
    private final int       lengthFieldOffset;
    // 代表长度字段自身的长度。支持1,2,4.如果是1则使用unsignedbyte方式读取。如果是2则使用unsignedshort方式读取,4使用int方式读取。
    private final int       lengthFieldLength;
    // 将长度字段读取完毕，需要的偏移量,就是上面两个值相加
    private final int       lengthFieldEndOffset;
    // 需要忽略的字节数
    private final int       skipBytes;
    private final int       maxLegnth;
    private BufferAllocator allocator;
    
    /**
     * 
     * @param lengthFieldOffset 长度字段在报文中的偏移量
     * @param lengthFieldLength 长度字段本身的长度
     * @param skipBytes 解析后的报文需要跳过的位数
     * @param maxLength
     */
    public TotalLengthFieldBasedFrameDecoderByHeap(int lengthFieldOffset, int lengthFieldLength, int skipBytes, int maxLength, BufferAllocator allocator)
    {
        this.lengthFieldOffset = lengthFieldOffset;
        this.lengthFieldLength = lengthFieldLength;
        this.maxLegnth = maxLength;
        this.skipBytes = skipBytes;
        lengthFieldEndOffset = lengthFieldOffset + lengthFieldLength;
        this.allocator = allocator;
    }
    
    @Override
    public void bind(ChannelContext channelContext)
    {
        
    }
    
    @Override
    public void process(IoBuffer ioBuffer, ProcessorInvoker next) throws Throwable
    {
        do
        {
            int maskReadPosi = ioBuffer.getReadPosi();
            int left = ioBuffer.remainRead();
            if (left == 0)
            {
                ioBuffer.compact().capacityReadyFor(lengthFieldEndOffset);
                return;
            }
            if (lengthFieldEndOffset > left)
            {
                ioBuffer.compact().capacityReadyFor(lengthFieldEndOffset);
                return;
            }
            // iobuffer中可能包含好几个报文，所以这里应该是增加的方式而不是直接设置的方式
            ioBuffer.addReadPosi(lengthFieldOffset);
            // 获取到整体报文的长度
            int length = 0;
            switch (lengthFieldLength)
            {
                case 1:
                    length = ioBuffer.get() & 0xff;
                    break;
                case 2:
                    length = ioBuffer.getShort() & 0xff;
                    break;
                case 4:
                    length = ioBuffer.getInt();
                    break;
            }
            // 得到整体长度后，开始从头读取这个长度的内容
            ioBuffer.setReadPosi(maskReadPosi);
            if (length >= maxLegnth)
            {
                throw new TooLongException();
            }
            if (length > ioBuffer.remainRead())
            {
                ioBuffer.compact().capacityReadyFor(length);
                return;
            }
            else
            {
                IoBuffer packet = allocator.heapBuffer(length);
                packet.put(ioBuffer, length);
                ioBuffer.addReadPosi(length);
                if (skipBytes != 0)
                {
                    packet.addReadPosi(skipBytes);
                }
                next.process(packet);
            }
        } while (true);
    }
    
    @Override
    public boolean backpressureProcess(IoBuffer ioBuffer, ProcessorInvoker next) throws Throwable
    {
        do
        {
            int maskReadPosi = ioBuffer.getReadPosi();
            int left = ioBuffer.remainRead();
            if (left == 0)
            {
                ioBuffer.compact().capacityReadyFor(lengthFieldEndOffset);
                return true;
            }
            if (lengthFieldEndOffset > left)
            {
                ioBuffer.compact().capacityReadyFor(lengthFieldEndOffset);
                return true;
            }
            // iobuffer中可能包含好几个报文，所以这里应该是增加的方式而不是直接设置的方式
            ioBuffer.addReadPosi(lengthFieldOffset);
            // 获取到整体报文的长度
            int length = 0;
            switch (lengthFieldLength)
            {
                case 1:
                    length = ioBuffer.get() & 0xff;
                    break;
                case 2:
                    length = ioBuffer.getShort() & 0xff;
                    break;
                case 4:
                    length = ioBuffer.getInt();
                    break;
            }
            // 得到整体长度后，开始从头读取这个长度的内容
            ioBuffer.setReadPosi(maskReadPosi);
            if (length >= maxLegnth)
            {
                throw new TooLongException();
            }
            if (length > ioBuffer.remainRead())
            {
                ioBuffer.compact().capacityReadyFor(length);
                return true;
            }
            else
            {
                IoBuffer packet = allocator.heapBuffer(length);
                packet.put(ioBuffer, length);
                ioBuffer.addReadPosi(length);
                if (skipBytes != 0)
                {
                    packet.addReadPosi(skipBytes);
                }
                boolean process = next.backPressureProcess(packet);
                if (process == false)
                {
                    return false;
                }
            }
        } while (true);
    }
    
}
