package com.jfireframework.jnet.common.util;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.Queue;
import java.util.concurrent.locks.LockSupport;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.baseutil.concurrent.MPSCQueue;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.Configuration;
import com.jfireframework.jnet.common.api.ReadProcessor;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.channelcontext.BaseChannelContext;
import com.jfireframework.jnet.common.decodec.FrameDecodec;
import com.jfireframework.jnet.common.streamprocessor.ProcesserUtil;
import com.jfireframework.jnet.common.streamprocessor.ProcessorTask;
import com.jfireframework.jnet.common.streamprocessor.StreamProcessor;

public class MutliAttachConfiguration implements Configuration
{
    protected AioListener               aioListener;
    protected FrameDecodec              frameDecodec;
    protected StreamProcessor[]         inProcessors;
    protected StreamProcessor[]         outProcessors;
    private int                         maxMerge = 10;
    protected AsynchronousSocketChannel socketChannel;
    private SendBufStorage              sendBufStorage;
    private ByteBuf<?>                  inCachedBuf;
    private ByteBuf<?>                  outCachedBuf;
    private ReadProcessor               readProcessor;
    
    public MutliAttachConfiguration(AioListener aioListener, FrameDecodec frameDecodec, StreamProcessor[] inProcessors, StreamProcessor[] outProcessors, int maxMerge, AsynchronousSocketChannel socketChannel, SendBufStorage sendBufStorage, ByteBuf<?> inCachedBuf, ByteBuf<?> outCachedBuf)
    {
        this.aioListener = aioListener;
        this.frameDecodec = frameDecodec;
        this.inProcessors = inProcessors;
        this.outProcessors = outProcessors;
        this.maxMerge = maxMerge;
        this.socketChannel = socketChannel;
        this.sendBufStorage = sendBufStorage;
        this.inCachedBuf = inCachedBuf;
        this.outCachedBuf = outCachedBuf;
        readProcessor = new ReadProcessor() {
            
            @Override
            public void process(ByteBuf<?> buf, SendBufStorage bufStorage, StreamProcessor[] inProcessors, ChannelContext channelContext) throws Throwable
            {
                ProcessorTask task = new ProcessorTask(buf, 0, channelContext);
                processor.commit(task);
            }
        };
    }
    
    @Override
    public ChannelContext config()
    {
        return new BaseChannelContext(readProcessor, sendBufStorage, maxMerge, aioListener, inProcessors, outProcessors, socketChannel, frameDecodec, inCachedBuf, outCachedBuf);
    }
    
    class MutlisAttachProcessor implements Runnable
    {
        private final Queue<ProcessorTask> tasks          = new MPSCQueue<>();
        private static final int           IDLE           = 0;
        private static final int           WORK           = 1;
        private final CpuCachePadingInt    status         = new CpuCachePadingInt(WORK);
        private static final int           SPIN_THRESHOLD = 1 << 7;
        private int                        spin           = 0;
        private volatile Thread            owner;
        
        @Override
        public void run()
        {
            status.set(WORK);
            owner = Thread.currentThread();
            do
            {
                ProcessorTask task = tasks.poll();
                if (task == null)
                {
                    spin = 0;
                    for (;;)
                    {
                        
                        if ((task = tasks.poll()) != null)
                        {
                            break;
                        }
                        else if ((spin += 1) < SPIN_THRESHOLD)
                        {
                            ;
                        }
                        else
                        {
                            spin = 0;
                            status.set(IDLE);
                            if ((task = tasks.poll()) != null)
                            {
                                status.set(WORK);
                                break;
                            }
                            else
                            {
                                LockSupport.park();
                                status.set(WORK);
                            }
                        }
                    }
                }
                try
                {
                    ChannelContext serverChannelContext = task.getChannelContext();
                    if (serverChannelContext.isOpen())
                    {
                        Object result = ProcesserUtil.process(serverChannelContext, serverChannelContext.inProcessors(), task.getData(), task.getInitIndex());
                        if (result instanceof ByteBuf<?>)
                        {
                            serverChannelContext.sendBufStorage().putBuf((ByteBuf<?>) result);
                            serverChannelContext.registerWrite();
                        }
                    }
                }
                catch (Throwable e)
                {
                    aioListener.catchException(e, task.getChannelContext());
                }
            } while (true);
        }
        
        public void commit(ProcessorTask task)
        {
            tasks.offer(task);
            tryExecute();
        }
        
        private void tryExecute()
        {
            int now = status.value();
            if (now == IDLE)
            {
                LockSupport.unpark(owner);
            }
        }
    }
    
}
