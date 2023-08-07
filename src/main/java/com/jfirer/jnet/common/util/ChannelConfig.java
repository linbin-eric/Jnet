package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.api.WorkerGroup;
import com.jfirer.jnet.common.buffer.LeakDetecter;
import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import lombok.Data;

@Data
public class ChannelConfig
{
    private             int             decrCountMax         = 2;
    private             int             minReceiveSize       = 16;
    private             int             maxReceiveSize       = 1024 * 1024 * 8;
    private             int             initReceiveSize      = 1024;
    private             int             maxBatchWrite        = 1024 * 1024 * 8;
    private             int             msOfReadTimeout      = 1000 * 60 * 5;
    private             String          ip                   = "0.0.0.0";
    private             int             port                 = -1;
    private             int             backLog              = 50;
    private             BufferAllocator allocator;
    private             int             channelThreadNum;
    private             String          channelTreadNamePrefix;
    private             WorkerGroup     workerGroup;
    public static final LeakDetecter    IoBufferLeakDetected = new LeakDetecter(System.getProperty("Leak.Detect.IoBuffer") == null ? LeakDetecter.WatchLevel.none : LeakDetecter.WatchLevel.valueOf(System.getProperty("Leak.Detect.IoBuffer")));
}
