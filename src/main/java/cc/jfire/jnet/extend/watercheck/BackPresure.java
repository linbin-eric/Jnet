package cc.jfire.jnet.extend.watercheck;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.WriteListener;

import java.util.concurrent.atomic.AtomicInteger;

public record BackPresure(AtomicInteger counter, ReadProcessor<Void> readLimiter, WriteListener writeLimiter, int limit)
{
    public static String UP_STREAM_BACKPRESURE = "upstreamBackPresure";
    public static String IN_BACKPRESURE        = "inBackPresure";

    public static BackPresure noticeWaterLevel(int limit)
    {
        AtomicInteger      counter      = new AtomicInteger();
        NoticeReadLimiter  readLimiter  = new NoticeReadLimiter(counter, limit);
        NoticeWriteLimiter writeLimiter = new NoticeWriteLimiter(counter, readLimiter, limit);
        return new BackPresure(counter, readLimiter, writeLimiter, limit);
    }
}
