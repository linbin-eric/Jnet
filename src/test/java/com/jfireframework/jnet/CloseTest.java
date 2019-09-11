package com.jfireframework.jnet;

import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.*;
import com.jfireframework.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfireframework.jnet.common.internal.BindDownAndUpStreamDataProcessor;
import com.jfireframework.jnet.common.internal.DefaultAcceptHandler;
import com.jfireframework.jnet.common.processor.ChannelAttachProcessor;
import com.jfireframework.jnet.common.util.AioListenerAdapter;
import com.jfireframework.jnet.common.util.CapacityStat;
import com.jfireframework.jnet.common.util.ChannelConfig;
import com.jfireframework.jnet.server.AioServer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.Collection;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Parameterized.class)
public class CloseTest
{
    static enum IoMode
    {
        IO, Channel
    }

    private String ip       = "127.0.0.1";
    private int    port     = 4586;
    private int    writeNum = 100;

    @Parameters(name = "IO模式:{2}")
    public static Collection<Object[]> params()
    {
        return Arrays.asList(new Object[][]{ //
                {PooledUnThreadCacheBufferAllocator.DEFAULT, 1024 * 1024 * 2, IoMode.IO}, //
                {PooledUnThreadCacheBufferAllocator.DEFAULT, 1024 * 1024 * 2, IoMode.Channel}, //
        });
    }

    private BufferAllocator bufferAllocator;
    private int             batchWriteNum;
    private IoMode          ioMode;

    public CloseTest(final BufferAllocator bufferAllocator, int batchWriteNum, final IoMode ioMode) throws IOException, InterruptedException
    {
        this.bufferAllocator = bufferAllocator;
        this.batchWriteNum = batchWriteNum;
        this.ioMode = ioMode;
    }

    @Test
    public void test() throws Throwable
    {
        ChannelConfig channelConfig = new ChannelConfig();
        final ExecutorService fixService = Executors.newFixedThreadPool(4, new ThreadFactory()
        {
            AtomicInteger atomicInteger = new AtomicInteger(0);

            @Override
            public Thread newThread(Runnable r)
            {
                int count = atomicInteger.getAndIncrement();
                return new Thread(r, "channelWorker-" + (count));
            }
        });
        final CountDownLatch countDownLatch = new CountDownLatch(writeNum);
        AioListener aioListener = new AioListenerAdapter()
        {
            @Override
            public void onClose(ChannelContext channelContext, Throwable e)
            {
//                countDownLatch.countDown();
            }
        };
        final Queue<IoBuffer> queue = new ConcurrentLinkedQueue<>();
        final DataProcessor dataProcessor = new BindDownAndUpStreamDataProcessor<IoBuffer>()
        {
            @Override
            public void process(IoBuffer data) throws Throwable
            {
                if (data != null)
                {
                    data.addReadPosi(-4);
                    queue.add(data);
                    countDownLatch.countDown();
                }
                else
                {
                    for (IoBuffer each : queue)
                    {
                        downStream.process(each);
                    }
                    queue.clear();
                }
            }
        };
        ChannelContextInitializer initializer = new ChannelContextInitializer()
        {

            @Override
            public void onChannelContextInit(final ChannelContext channelContext)
            {
                switch (ioMode)
                {
                    case IO:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 5, bufferAllocator), //
                                dataProcessor);
                        break;
                    case Channel:
                        channelContext.setDataProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 5, bufferAllocator), //
                                new ChannelAttachProcessor(fixService), //
                                dataProcessor);
                        break;
                    default:
                        break;
                }
            }
        };
        channelConfig.setIp(ip);
        channelConfig.setPort(port);
        AioServer aioServer = AioServer.newAioServer(channelConfig,initializer);
        aioServer.start();
        Socket socket  = new Socket(ip, port);
        byte[] content = new byte[PooledBufferAllocator.PAGESIZE];
        content[0] = (byte) ((content.length >> 24) & 0xff);
        content[1] = (byte) ((content.length >> 16) & 0xff);
        content[2] = (byte) ((content.length >> 8) & 0xff);
        content[3] = (byte) ((content.length >> 0) & 0xff);
        OutputStream outputStream = socket.getOutputStream();
        for (int i = 0; i < writeNum; i++)
        {
            outputStream.write(content);
            outputStream.flush();
        }
        countDownLatch.await();
        CapacityStat internalStat = getStat((PooledBufferAllocator) bufferAllocator);
        Assert.assertTrue(internalStat.getChunkCapacity() - internalStat.getFreeBytes() >= PooledBufferAllocator.PAGESIZE * (writeNum + 1));
        outputStream.close();
        dataProcessor.process(null);
        socket.close();
        aioServer.termination();
        Thread.sleep(2000);
        CapacityStat stat = getStat((PooledBufferAllocator) bufferAllocator);
        Assert.assertEquals(0, stat.getChunkCapacity() - stat.getFreeBytes());
    }

    private CapacityStat getStat(PooledBufferAllocator bufferAllocator)
    {
        CapacityStat stat = new CapacityStat();
        for (DirectArena each : bufferAllocator.getDirectArenas())
        {
            each.capacityStat(stat);
        }
        for (HeapArena each : bufferAllocator.getHeapArenas())
        {
            each.capacityStat(stat);
        }
        return stat;
    }
}
