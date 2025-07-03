package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.IOException;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Data
@Accessors(chain = true)
public class ChannelConfig
{
    public static final LeakDetecter             IoBufferLeakDetected = new LeakDetecter(System.getProperty("Leak.Detect.IoBuffer") == null ? LeakDetecter.WatchLevel.none : LeakDetecter.WatchLevel.valueOf(System.getProperty("Leak.Detect.IoBuffer")));
    public static final AsynchronousChannelGroup DEFAULT_CHANNEL_GROUP;

    static
    {
        try
        {
            DEFAULT_CHANNEL_GROUP = AsynchronousChannelGroup.withThreadPool(Executors.newVirtualThreadPerTaskExecutor());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private int                       decrCountMax      = 2;
    private int                       minReceiveSize    = 16;
    private int                       maxReceiveSize    = 1024 * 1024 * 8;
    private int                       initReceiveSize   = 1024;
    private int                       maxBatchWrite     = 1024 * 1024 * 2;
    private String                    ip                = "0.0.0.0";
    private int                       port              = -1;
    private int                       backLog           = 50;
    private Consumer<Throwable>       jvmExistHandler   = e -> {
        System.err.println("Some RunnableImpl run in Jnet not handle Exception well,Check all ReadProcessor and WriteProcessor");
        e.printStackTrace();
    };
    private Supplier<BufferAllocator> allocatorSupplier = () ->new PooledBufferAllocator(5000, true, PooledBufferAllocator.getArena(true));
//    private Supplier<BufferAllocator> allocatorSupplier = () ->new UnPoolBufferAllocator(true);
    private AsynchronousChannelGroup  channelGroup      = DEFAULT_CHANNEL_GROUP;
}
