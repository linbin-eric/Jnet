package com.jfirer.jnet;

import com.jfirer.jnet.common.api.*;
import com.jfirer.jnet.common.buffer.*;
import com.jfirer.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfirer.jnet.common.util.CapacityStat;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;
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

    private String ip       = "127.0.0.1";
    private int    port     = 4586;
    private int    writeNum = 100;

    @Parameters(name = "IO模式:{2}")
    public static Collection<Object[]> params()
    {
        return Arrays.asList(new Object[][]{ //
                {PooledUnThreadCacheBufferAllocator.DEFAULT}, //
                {PooledUnThreadCacheBufferAllocator.DEFAULT}, //
        });
    }

    private BufferAllocator bufferAllocator;

    public CloseTest(final BufferAllocator bufferAllocator) throws IOException, InterruptedException
    {
        this.bufferAllocator = bufferAllocator;
    }

    @Test
    public void test() throws Throwable
    {
        ChannelConfig channelConfig = new ChannelConfig();
        final CountDownLatch  countDownLatch = new CountDownLatch(writeNum);
        final Queue<IoBuffer> queue          = new ConcurrentLinkedQueue<>();
        final ReadProcessor dataProcessor = new ReadProcessor<IoBuffer>()
        {
            ProcessorContext ctx;

            @Override
            public void read(IoBuffer data, ProcessorContext ctx)
            {
                if (data != null)
                {
                    this.ctx = ctx;
                    data.addReadPosi(-4);
                    queue.add(data);
                    countDownLatch.countDown();
                }
                else
                {
                    for (IoBuffer each : queue)
                    {
                        this.ctx.fireRead(each);
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
                Pipeline pipeline = channelContext.pipeline();
                pipeline.add(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 5, bufferAllocator));
                pipeline.add(dataProcessor);
            }
        };
        channelConfig.setIp(ip);
        channelConfig.setPort(port);
        AioServer aioServer = AioServer.newAioServer(channelConfig, initializer);
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
        dataProcessor.read(null, null);
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
