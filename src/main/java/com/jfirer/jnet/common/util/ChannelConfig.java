package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.api.WorkerGroup;
import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ThreadFactory;

public class ChannelConfig
{
    private             BufferAllocator          allocator;
    private             int                      decrCountMax         = 2;
    private             int                      minReceiveSize       = 16;
    private             int                      maxReceiveSize       = 1024 * 1024 * 8;
    private             int                      initReceiveSize      = 1024;
    private             int                      maxBatchWrite        = 1024 * 1024 * 8;
    private             String                   ip                   = "0.0.0.0";
    private             int                      port                 = -1;
    private             int                      backLog              = 50;
    private             AsynchronousChannelGroup channelGroup;
    private             WorkerGroup              workerGroup;
    public static final LeakDetecter             IoBufferLeakDetected = new LeakDetecter(System.getProperty("Leak.Detect.IoBuffer") == null ? LeakDetecter.WatchLevel.none : LeakDetecter.WatchLevel.valueOf(System.getProperty("Leak.Detect.IoBuffer")));
    public static final AsynchronousChannelGroup DEFAULT_CHANNEL_GROUP;
    public static final WorkerGroup              DEFAULT_WORKER_GROUP = new DefaultWorkerGroup();
    static
    {
        AsynchronousChannelGroup channelGroup;
        try
        {
            channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory()
            {
                int i = 1;

                @Override
                public Thread newThread(Runnable r)
                {
                    return new FastThreadLocalThread(r, "AioServer_IoWorker-" + (i++));
                }
            });
        }
        catch (IOException e)
        {
            channelGroup = null;
            ReflectUtil.throwException(e);
        }
        DEFAULT_CHANNEL_GROUP = channelGroup;
    }
    public AsynchronousChannelGroup getChannelGroup()
    {
        if (channelGroup == null)
        {
            try
            {
                channelGroup = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), new ThreadFactory()
                {
                    int i = 1;

                    @Override
                    public Thread newThread(Runnable r)
                    {
                        return new FastThreadLocalThread(r, "AioServer_IoWorker-" + (i++));
                    }
                });
            }
            catch (IOException e)
            {
                ReflectUtil.throwException(e);
            }
        }
        return channelGroup;
    }

    public void setChannelGroup(AsynchronousChannelGroup channelGroup)
    {
        this.channelGroup = channelGroup;
    }

    public int getPort()
    {
        return port;
    }

    public void setPort(int port)
    {
        this.port = port;
    }

    public int getBackLog()
    {
        return backLog;
    }

    public void setBackLog(int backLog)
    {
        this.backLog = backLog;
    }

    public String getIp()
    {
        return ip;
    }

    public void setIp(String ip)
    {
        this.ip = ip;
    }

    public int getMaxBatchWrite()
    {
        return maxBatchWrite;
    }

    public void setMaxBatchWrite(int maxBatchWrite)
    {
        this.maxBatchWrite = maxBatchWrite;
    }

    public BufferAllocator getAllocator()
    {
        if (allocator == null)
        {
            allocator = PooledBufferAllocator.DEFAULT;
        }
        return allocator;
    }

    public void setAllocator(BufferAllocator allocator)
    {
        this.allocator = allocator;
    }

    public int getMinReceiveSize()
    {
        return minReceiveSize;
    }

    public void setMinReceiveSize(int minReceiveSize)
    {
        this.minReceiveSize = minReceiveSize;
    }

    public int getMaxReceiveSize()
    {
        return maxReceiveSize;
    }

    public void setMaxReceiveSize(int maxReceiveSize)
    {
        this.maxReceiveSize = maxReceiveSize;
    }

    public int getInitReceiveSize()
    {
        return initReceiveSize;
    }

    public void setInitReceiveSize(int initReceiveSize)
    {
        this.initReceiveSize = initReceiveSize;
    }

    public synchronized WorkerGroup getWorkerGroup()
    {
        if (workerGroup == null)
        {
            workerGroup = DEFAULT_WORKER_GROUP;
        }
        return workerGroup;
    }

    public void setWorkerGroup(WorkerGroup workerGroup)
    {
        this.workerGroup = workerGroup;
    }

    public int getDecrCountMax()
    {
        return decrCountMax;
    }

    public void setDecrCountMax(int decrCountMax)
    {
        this.decrCountMax = decrCountMax;
    }
}
