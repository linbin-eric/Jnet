package com.jfirer.jnet.extend.watercheck;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class ReadLimiter extends AbstractReadLimiter
{
    @Getter
    private final       AtomicLong counter         = new AtomicLong();
    public static final long       MAX_QUEUED_SIZE = 1024 * 1024 * 50;

    public ReadLimiter()
    {
        super(MAX_QUEUED_SIZE);
    }

    @Override
    public long getLimitSize()
    {
        return counter.get();
    }

    public ReadLimiter associated(WriteLimiter writeLimiter)
    {
        writeLimiter.associated(this);
        return this;
    }
}
