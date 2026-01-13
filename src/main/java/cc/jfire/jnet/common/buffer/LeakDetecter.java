package cc.jfire.jnet.common.buffer;

import lombok.Getter;
import lombok.Setter;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class LeakDetecter
{
    final         Object                                 dummy     = new Object();
    private final WatchLevel                             watchLevel;
    //Refence对象如果自身被GC了，就不会被放入到队列中。因此需要有一个地方持有他们的强引用。
    private final ConcurrentHashMap<LeakTracker, Object> map       = new ConcurrentHashMap<>();
    final         LeakTracker                            leakDummy = new LeakTracker(null, null, map, false);
    private final ReferenceQueue<Object>                 queue     = new ReferenceQueue<>();

    public LeakDetecter(WatchLevel watchLevel)
    {
        this.watchLevel = watchLevel;
        new Thread(() -> {
            while (true)
            {
                try
                {
                    LeakTracker reference = (LeakTracker) queue.remove();
                    map.remove(reference);
                    if (reference.isClose())
                    {
                        ;
                    }
                    else
                    {
                    }
                    reference.clear();
                }
                catch (Throwable e)
                {
                }
            }
        }).start();
    }

    public LeakTracker watch(Object entity, int stackTraceLevel)
    {
        LeakTracker tracker;
        switch (watchLevel)
        {
            case none -> tracker = leakDummy;
            case sample -> tracker = ThreadLocalRandom.current().nextInt(100) == 0 ? buildTracker(entity, stackTraceLevel) : leakDummy;
            case all -> tracker = buildTracker(entity, stackTraceLevel);
            default -> throw new IllegalStateException("Unexpected value: " + watchLevel);
        }
        return tracker;
    }

    private LeakTracker buildTracker(Object entity, int stackTraceLevel)
    {
        LeakTracker tracker = new LeakTracker(entity, queue, map, true);
        map.put(tracker, dummy);
        tracker.setSource(Arrays.stream(Thread.currentThread().getStackTrace()).skip(3).limit(stackTraceLevel).map(stackTraceElement -> "[" + Thread.currentThread().getName() + "]:" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + ":" + stackTraceElement.getLineNumber()).collect(Collectors.joining("\r\n")));
        return tracker;
    }

    public enum WatchLevel
    {
        none, sample, all
    }

    @Setter
    @Getter
    public static class LeakTracker extends PhantomReference<Object>
    {
        private String                   source;
        private boolean                  close = false;
        private boolean                  watchTrace;
        private Set<String>              traceQueue;
        private Map<LeakTracker, Object> map;

        public LeakTracker(Object referent, ReferenceQueue<Object> q, Map<LeakTracker, Object> map, boolean watchTrace)
        {
            super(referent, q);
            this.map        = map;
            this.watchTrace = watchTrace;
            if (watchTrace)
            {
                traceQueue = new ConcurrentSkipListSet<>();
            }
        }

        public void close()
        {
            close = true;
            map.remove(this);
        }

        public void addInvokeTrace(int skip, int limit)
        {
            if (watchTrace)
            {
                traceQueue.add(Arrays.stream(Thread.currentThread().getStackTrace()).skip(skip).limit(Math.min(limit, 9)).map(stackTraceElement -> "[" + Thread.currentThread().getName() + "]:" + stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + ":" + stackTraceElement.getLineNumber()).collect(Collectors.joining("\r\n")));
            }
        }
    }
}
