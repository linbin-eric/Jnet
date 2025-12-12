package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;

import java.util.concurrent.atomic.AtomicLong;

public class WriteLimiter implements WriteListener
{
    private volatile AtomicLong  counter;
    private volatile ReadLimiter readLimiter;

    @Override
    public void queuedWrite(long size)
    {
        AtomicLong c = counter;
        if (c != null)
        {
            c.addAndGet(size);
        }
    }

    @Override
    public void partWriteFinish(long currentSend)
    {
        AtomicLong c = counter;
        if (c != null)
        {
            long        left = c.addAndGet(0 - currentSend);
            ReadLimiter r    = readLimiter;
            if (left < ReadLimiter.MAX_QUEUED_SIZE && r != null)
            {
                r.tryRegisterRead();
            }
        }
    }

    @Override
    public void writeFailed(Throwable e)
    {
        ReadLimiter r = readLimiter;
        if (r != null)
        {
            r.tryRegisterRead();
        }
    }

    public WriteLimiter associated(ReadLimiter readLimiter)
    {
        this.readLimiter = readLimiter;
        counter          = readLimiter.getCounter();
        return this;
    }
}
