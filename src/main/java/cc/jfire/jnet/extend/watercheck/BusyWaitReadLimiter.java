package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public record BusyWaitReadLimiter(AtomicInteger count, int limit) implements ReadProcessor<Object>
{
    @Override
    public void read(Object data, ReadProcessorNode next)
    {
    }

    @Override
    public void readCompleted(ReadProcessorNode next)
    {
        if (count.get() < limit)
        {
            next.fireReadCompleted();
        }
        else
        {
            int spinCount = 0;
            int parkCount = 0;
            while (count.get() >= limit)
            {
                spinCount += 1;
                Thread.onSpinWait();
                if (spinCount > 16)
                {
                    spinCount = 0;
                    park(parkCount++);
                }
            }
            next.fireReadCompleted();
        }
    }

    private void park(int i)
    {
        if (i < 2)
        {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(50));
        }
        else if (i < 8)
        {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(100));
        }
        else
        {
            LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(1));
        }
    }
}
