package com.jfirer.jnet.extend.watercheck;

import com.jfirer.jnet.common.api.PartWriteFinishCallback;
import com.jfirer.jnet.common.api.WriteProcessor;
import com.jfirer.jnet.common.api.WriteProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.util.concurrent.atomic.AtomicLong;

@Data
public class WriteLimiter implements WriteProcessor<IoBuffer>, PartWriteFinishCallback
{
    private final AtomicLong COUNTER;
    private final long        LIMIT;
    private final ReadLimiter readLimiter;

    @Override
    public void write(IoBuffer data, WriteProcessorNode next)
    {
        COUNTER.addAndGet(data.remainRead());
        next.fireWrite(data);
    }

    @Override
    public void partWriteFinish(long currentSend)
    {
        long left = COUNTER.addAndGet(0 - currentSend);
        if (left < LIMIT)
        {
            readLimiter.tryRegisterRead();
        }
    }

    @Override
    public void writeFailed(Throwable e)
    {
        readLimiter.tryRegisterRead();
    }
}
