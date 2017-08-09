package com.jfireframework.jnet.common.channelcontext;

import java.io.IOException;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.collection.buffer.DirectByteBuf;
import com.jfireframework.baseutil.resource.ResourceCloseAgent;
import com.jfireframework.baseutil.resource.ResourceCloseCallback;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ReadHandler;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.BufStorage;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.readhandler.DefaultReadHandler;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;
import com.jfireframework.jnet.common.writehandler.DefaultWriteHandler;

public abstract class BaseChannelContext implements ChannelContext
{
    protected ReadHandler                              readHandler;
    protected final ExecutorService                    businessExecutorService;
    protected final WriteHandler                       writeHandler;
    protected final AioListener                        aioListener;
    protected final FrameDecodec                       frameDecodec;
    protected final BufStorage                         bufStorage;
    protected final ByteBuf<?>                         ioBuf;
    protected final StreamProcessor[]                  inProcessors;
    protected final StreamProcessor[]                  outProcessors;
    protected final AsynchronousSocketChannel          socketChannel;
    protected final ResourceCloseAgent<ChannelContext> closeAgent = new ResourceCloseAgent<ChannelContext>(this, new ResourceCloseCallback<ChannelContext>() {
                                                                      
                                                                      @Override
                                                                      public void onClose(ChannelContext resource)
                                                                      {
                                                                          try
                                                                          {
                                                                              socketChannel.close();
                                                                          }
                                                                          catch (IOException e)
                                                                          {
                                                                              e.printStackTrace();
                                                                          }
                                                                          finally
                                                                          {
                                                                              ioBuf.release();
                                                                          }
                                                                      }
                                                                  });
    
    public BaseChannelContext(//
            ExecutorService businessExecutorService, //
            BufStorage bufStorage, //
            int maxMerge, //
            AioListener aioListener, //
            StreamProcessor[] inProcessors, //
            StreamProcessor[] outProcessors, //
            AsynchronousSocketChannel socketChannel, //
            FrameDecodec frameDecodec)
    {
        this.businessExecutorService = businessExecutorService;
        this.socketChannel = socketChannel;
        this.inProcessors = inProcessors;
        this.outProcessors = outProcessors;
        this.bufStorage = bufStorage;
        this.aioListener = aioListener;
        this.frameDecodec = frameDecodec;
        ioBuf = DirectByteBuf.allocate(128);
        writeHandler = new DefaultWriteHandler(maxMerge, socketChannel, aioListener, bufStorage, this);
    }
    
    protected abstract ReadProcessor buildReadProcessor(ExecutorService businessExecutorService, AioListener serverListener, ChannelContext channelContext, BufStorage bufStorage, WriteHandler writeHandler, StreamProcessor[] inProcessors);
    
    @Override
    public void push(Object send, int index) throws Throwable
    {
        Object finalResult = ProcesserUtil.process(this, outProcessors, send, index);
        if (finalResult instanceof ByteBuf<?>)
        {
            bufStorage.putBuf((ByteBuf<?>) finalResult);
            writeHandler.registerWrite();
        }
    }
    
    @Override
    public void registerWrite()
    {
        writeHandler.registerWrite();
    }
    
    @Override
    public boolean close()
    {
        return closeAgent.close();
    }
    
    @Override
    public boolean isOpen()
    {
        return socketChannel.isOpen();
    }
    
    @Override
    public StreamProcessor[] inProcessors()
    {
        return inProcessors;
    }
    
    @Override
    public StreamProcessor[] outProcessors()
    {
        return outProcessors;
    }
    
    @Override
    public BufStorage bufStorage()
    {
        return bufStorage;
    }
    
    @Override
    public AsynchronousSocketChannel socketChannel()
    {
        return socketChannel;
    }
    
    @Override
    public void registerRead()
    {
        readHandler = new DefaultReadHandler(buildReadProcessor(businessExecutorService, aioListener, this, bufStorage, writeHandler, inProcessors), socketChannel, frameDecodec, ioBuf, aioListener, this);
        readHandler.registerRead();
    }
    
}
