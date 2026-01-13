package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;

import java.util.concurrent.atomic.AtomicInteger;

public record BusyWaitWriteLimiter(AtomicInteger counter) implements WriteListener
{
    @Override
    public void partWriteFinish(int currentSend)
    {
        counter.addAndGet(-currentSend);
    }

    @Override
    public void queuedWrite(int size)
    {
        counter.addAndGet(size);
    }

    @Override
    public void writeFailed(Throwable e)
    {
        counter.set(0);
    }
}
