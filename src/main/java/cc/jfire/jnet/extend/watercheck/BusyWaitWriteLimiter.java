package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BusyWaitWriteLimiter implements WriteListener
{
    private final AtomicInteger counter;

    @Override
    public void partWriteFinish(int currentSend)
    {
        counter.addAndGet(0 - currentSend);
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
