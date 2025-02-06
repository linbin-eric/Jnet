package com.jfirer.jnet;

import com.jfirer.jnet.common.api.InternalPipeline;
import com.jfirer.jnet.common.api.PipelineInitializer;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.decoder.TotalLengthFieldBasedFrameDecoder;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;

@RunWith(Parameterized.class)
public class CloseTest
{
    private final String          ip       = "127.0.0.1";
    private final int             port     = 4586;
    private final int             writeNum = 100;
    private final BufferAllocator bufferAllocator;

    public CloseTest(final BufferAllocator bufferAllocator) throws IOException, InterruptedException
    {
        this.bufferAllocator = bufferAllocator;
    }

    @Parameters(name = "IO模式:{2}")
    public static Collection<Object[]> params()
    {
        return Arrays.asList(new Object[][]{ //
//                {PooledBufferAllocator.DEFAULT}
                {new PooledBufferAllocator("closetest")}, //
//                {PooledUnThreadCacheBufferAllocator.DEFAULT}, //
        });
    }

    @Test
    public void test() throws Throwable
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setWorkerGroup(new DefaultWorkerGroup(2, "close_"));
        channelConfig.setChannelGroup(ChannelConfig.DEFAULT_CHANNEL_GROUP);
        channelConfig.setMinReceiveSize(PooledBufferAllocator.PAGESIZE);
        channelConfig.setAllocator(bufferAllocator);
        final CountDownLatch  countDownLatch = new CountDownLatch(writeNum);
        final Queue<IoBuffer> queue          = new ConcurrentLinkedQueue<>();
        final DataProcessor   dataProcessor  = new DataProcessor(queue, countDownLatch);
        PipelineInitializer initializer = pipeline -> {
            pipeline.addReadProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024 * 1024 * 5));
            pipeline.addReadProcessor(dataProcessor);
            dataProcessor.pipeline = (InternalPipeline) pipeline;
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
            Thread.sleep(10);
        }
        countDownLatch.await();
        Thread.sleep(1000);
        CapacityStat internalStat = getStat((PooledBufferAllocator) bufferAllocator);
        Assert.assertTrue(internalStat.getChunkCapacity() - internalStat.getFreeBytes() >= PooledBufferAllocator.PAGESIZE * (writeNum + 1));
        outputStream.close();
        System.out.println("切分点");
        Thread.sleep(2000);
        dataProcessor.read(null, null);
        socket.close();
        aioServer.termination();
        Thread.sleep(100);
        System.out.println("sss");
        CapacityStat stat = getStat((PooledBufferAllocator) bufferAllocator);
        Assert.assertEquals(0, stat.getChunkCapacity() - stat.getFreeBytes());
    }

    private CapacityStat getStat(PooledBufferAllocator bufferAllocator)
    {
        CapacityStat stat = new CapacityStat();
        bufferAllocator.directCapacityStat(stat);
        bufferAllocator.heapCapacityStat(stat);
        return stat;
    }

    class DataProcessor implements ReadProcessor<IoBuffer>
    {
        final Queue<IoBuffer> queue;
        final CountDownLatch  countDownLatch;
        InternalPipeline pipeline;

        public DataProcessor(Queue<IoBuffer> queue, CountDownLatch countDownLatch)
        {
            this.queue          = queue;
            this.countDownLatch = countDownLatch;
        }

        @Override
        public void read(IoBuffer data, ReadProcessorNode next)
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
                    pipeline.fireWrite(each);
                }
                queue.clear();
            }
        }
    }
}
