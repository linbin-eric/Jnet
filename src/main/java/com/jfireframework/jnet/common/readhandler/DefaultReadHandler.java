package com.jfireframework.jnet.common.readhandler;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadHandler;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.decodec.DecodeResult;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.exception.EndOfStreamException;
import com.jfireframework.jnet.common.exception.NotFitProtocolException;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class DefaultReadHandler implements ReadHandler
{
    protected final FrameDecodec              frameDecodec;
    protected final ByteBuf<?>                ioBuf;
    protected final ChannelContext            channelContext;
    protected final AsynchronousSocketChannel socketChannel;
    protected final AioListener               channelLisntener;
    protected volatile boolean                readPending = false;
    protected final ReadProcessor             readProcessor;
    protected final BufStorage                bufStorage;
    protected final StreamProcessor[]         inProcessors;
    
    public DefaultReadHandler(ReadProcessor readProcessor, AsynchronousSocketChannel socketChannel, FrameDecodec frameDecodec, ByteBuf<?> ioBuf, AioListener channelListener, ChannelContext channelContext)
    {
        this.socketChannel = socketChannel;
        this.frameDecodec = frameDecodec;
        this.ioBuf = ioBuf;
        this.channelLisntener = channelListener;
        this.channelContext = channelContext;
        this.readProcessor = readProcessor;
        bufStorage = channelContext.bufStorage();
        inProcessors = channelContext.inProcessors();
    }
    
    @Override
    public void completed(Integer read, Void nothing)
    {
        if (read == -1)
        {
            catchThrowable(EndOfStreamException.instance, channelContext);
            return;
        }
        ioBuf.addWriteIndex(read);
        try
        {
            decodecAndProcess();
            socketChannel.read(getWriteBuffer(), null, this);
        }
        catch (Throwable e)
        {
            catchThrowable(e, channelContext);
        }
    }
    
    @Override
    public void failed(Throwable exc, Void nothing)
    {
        catchThrowable(exc, channelContext);
    }
    
    public void decodecAndProcess() throws Throwable
    {
        while (true)
        {
            DecodeResult decodeResult = frameDecodec.decodec(ioBuf);
            switch (decodeResult.getType())
            {
                case LESS_THAN_PROTOCOL:
                    return;
                case BUF_NOT_ENOUGH:
                    ioBuf.compact().ensureCapacity(decodeResult.getNeed());
                    return;
                case NOT_FIT_PROTOCOL:
                    catchThrowable(NotFitProtocolException.instance, channelContext);
                    return;
                case NORMAL:
                    ByteBuf<?> packet = decodeResult.getBuf();
                    readProcessor.process(packet, bufStorage, inProcessors, channelContext);
            }
        }
    }
    
    /**
     * 将iobuf的内容进行压缩，返回一个处于可写状态的ByteBuffer
     * 
     * @return
     */
    protected ByteBuffer getWriteBuffer()
    {
        ByteBuffer ioBuffer = ioBuf.nioBuffer();
        ioBuffer.position(ioBuffer.limit()).limit(ioBuffer.capacity());
        return ioBuffer;
    }
    
    protected void catchThrowable(Throwable e, ChannelContext context)
    {
        channelLisntener.catchException(e, context);
    }
    
    public void registerRead()
    {
        if (readPending)
        {
            throw new UnsupportedOperationException();
        }
        readPending = true;
        channelLisntener.readRegister(channelContext);
        try
        {
            socketChannel.read(getWriteBuffer(), null, this);
        }
        catch (Exception e)
        {
            channelLisntener.catchException(e, channelContext);
        }
    }
    
}
