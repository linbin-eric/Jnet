package com.jfirer.jnet.common.buffer;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.ref.PhantomReference;
import java.lang.ref.ReferenceQueue;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
public class LeakDetecter
{
    public enum WatchLevel
    {
        none,
        sample,
        all
    }

    private final WatchLevel                             watchLevel;
    //Refence对象如果自身被GC了，就不会被放入到队列中。因此需要有一个地方持有他们的强引用。
    private final ConcurrentHashMap<LeakTracker, Object> map       = new ConcurrentHashMap<>();
    final         Object                                 dummy     = new Object();
    final         LeakTracker                            leakDummy = new LeakTracker(null, null);

    public LeakDetecter(WatchLevel watchLevel)
    {
        System.out.println("资源泄露监控级别：" + watchLevel);
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
                        if (reference.getSource() != null)
                        {
                            log.error("发现资源泄露，泄露资源的创建栈如下:\r\n{}", reference.getSource());
                        }
                        else
                        {
                            log.error("发现资源泄露");
                        }
                    }
                    reference.clear();
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private ReferenceQueue<Object> queue = new ReferenceQueue<>();

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
        LeakTracker tracker = new LeakTracker(entity, queue);
        map.put(tracker, dummy);
        tracker.setSource(Arrays.stream(Thread.currentThread().getStackTrace()).skip(3).limit(stackTraceLevel).map(stackTraceElement -> stackTraceElement.getClassName() + "." + stackTraceElement.getMethodName() + ":" + stackTraceElement.getLineNumber()).collect(Collectors.joining("\r\n")));
        return tracker;
    }

    @Setter
    @Getter
    public static class LeakTracker extends PhantomReference<Object>
    {
        private String  source;
        private boolean close = false;

        public LeakTracker(Object referent, ReferenceQueue<Object> q)
        {
            super(referent, q);
        }

        public void close()
        {
            close = true;
        }
    }
}
