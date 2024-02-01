package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.api.WorkerGroup;
import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import lombok.Data;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public class ChannelConfig
{
    private             int                      decrCountMax          = 2;
    private             int                      minReceiveSize        = 16;
    private             int                      maxReceiveSize        = 1024 * 1024 * 8;
    private             int                      initReceiveSize       = 1024;
    private             int                      maxBatchWrite         = 1024 * 1024 * 8;
    private             int                      msOfReadTimeout       = 1000 * 60 * 5;
    private             String                   ip                    = "0.0.0.0";
    private             int                      port                  = -1;
    private             int                      backLog               = 50;
    private             BufferAllocator          allocator             = PooledBufferAllocator.DEFAULT;
    private             AsynchronousChannelGroup channelGroup;
    private             WorkerGroup              workerGroup;
    private             boolean                  IO_USE_CURRENT_THREAD = Integer.parseInt(System.getProperty("java.specification.version")) >= 21;
    public static final LeakDetecter             IoBufferLeakDetected  = new LeakDetecter(System.getProperty("Leak.Detect.IoBuffer") == null ? LeakDetecter.WatchLevel.none : LeakDetecter.WatchLevel.valueOf(System.getProperty("Leak.Detect.IoBuffer")));
    public static final AsynchronousChannelGroup DEFAULT_CHANNEL_GROUP;
    public static final WorkerGroup              DEFAULT_WORKER_GROUP  = new DefaultWorkerGroup(Runtime.getRuntime().availableProcessors(), "default_JnetWorker_");

    static
    {
        //大于 JDK21 默认启用虚拟线程模式
        if (Integer.parseInt(System.getProperty("java.specification.version")) >= 21)
        {
            AsynchronousChannelGroup virtual_thread_channel_group_tmp;
            try
            {
                Method newVirtualThreadPerTaskExecutor = null;
                newVirtualThreadPerTaskExecutor = Executors.class.getDeclaredMethod("newVirtualThreadPerTaskExecutor");
                newVirtualThreadPerTaskExecutor.setAccessible(true);
                ExecutorService executorService = (ExecutorService) newVirtualThreadPerTaskExecutor.invoke(null);
                virtual_thread_channel_group_tmp = AsynchronousChannelGroup.withThreadPool(executorService);
            }
            catch (Throwable e)
            {
                throw new RuntimeException(e);
            }
            DEFAULT_CHANNEL_GROUP = virtual_thread_channel_group_tmp;
        }
        else
        {
            try
            {
                DEFAULT_CHANNEL_GROUP = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> new FastThreadLocalThread(r, "default_channelGroup_"));
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }
}
