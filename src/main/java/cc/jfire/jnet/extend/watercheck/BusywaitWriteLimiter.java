package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.WriteListener;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class BusywaitWriteLimiter implements WriteListener
{
    private final AtomicInteger counter ;

    @Override
    public void partWriteFinish(long currentSend)
    {
        counter.addAndGet((int) (0 - currentSend));
    }

    @Override
    public void queuedWrite(long size)
    {
        counter.addAndGet((int) size);
    }

    @Override
    public void writeFailed(Throwable e)
    {

    }
}
