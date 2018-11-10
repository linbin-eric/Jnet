package com.jfireframework.jnet.common.processor.worker;

import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import org.jctools.queues.ConcurrentCircularArrayQueue;
import org.jctools.queues.SpscArrayQueue;

import java.util.Queue;
import java.util.concurrent.ExecutorService;

public class ChannelAttachWorker implements Runnable
{

    private static final int              IDLE              = -1;
    private static final int              WORK              = 1;
    private static final int              blockByDownStream = 4;
    private static final int              SPIN_THRESHOLD    = 128;
    private static final long             STATE_OFFSET      = UNSAFE.getFieldOffset("state", ChannelAttachWorker.class);
    private final        ExecutorService  executorService;
    private static final int              capacity          = 1024;
    private              Queue<IoBuffer>  queue             = new SpscArrayQueue<>(capacity);
    private volatile     int              state             = IDLE;
    private              DataProcessor<?> upStream;
    private              DataProcessor    downStream;
    private              ChannelContext   channelContext;

    public ChannelAttachWorker(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    @Override
    public void run()
    {
//        System.out.println(Thread.currentThread().getName() + "启动channel");
        try
        {
            int spin = 0;
            do
            {
                IoBuffer avail = queue.poll();
                if (avail == null)
                {
                    spin = 0;
                    for (; ; )
                    {
                        if ((avail = queue.poll()) != null)
                        {
                            break;
                        }
                        else if ((spin += 1) < SPIN_THRESHOLD)
                        {
                            ;
                        }
                        else
                        {
                            state = IDLE;
//                            System.out.println(Thread.currentThread().getName() + "准备通知上游");
                            upStream.notifyedWriteAvailable();
                            if (queue.isEmpty() == false)
                            {
//                                System.out.println(Thread.currentThread().getName() + "唤醒后有数据了，再次争取");
                                tryExecute();
                            }
                            else
                            {
//                                System.out.println(Thread.currentThread().getName() + "没有数据，放弃");
                            }
                            return;
                        }
                    }
                }
//
                if (downStream.process(avail) == false)
                {
                    state = blockByDownStream;
                    if (downStream.canAccept())
                    {
                        recoverFromBlock();
                    }
                    else
                    {
//                        System.out.println(Thread.currentThread().getName() + "下游不可用，放弃");
                    }
                    break;
                }
//                System.out.println(Thread.currentThread().getName() + "消费" + avail);
            } while (true);
        } catch (Throwable e)
        {
            e.printStackTrace();
            channelContext.close(e);
        }
//        System.out.println(Thread.currentThread().getName() + "离开");
    }

    public boolean commit(ChannelContext channelContext, DataProcessor<?> downStream, Object data)
    {
        if (queue.offer((IoBuffer) data))
        {
//        System.out.println(Thread.currentThread().getName() + "提交数据，准备夺取");
            tryExecute();
            return true;
        }
        else
        {
            return false;
        }
    }

    public boolean canAccept()
    {
        long cIndex = ((ConcurrentCircularArrayQueue) queue).currentConsumerIndex();
        long pIndex = ((ConcurrentCircularArrayQueue) queue).currentProducerIndex();
        if (pIndex - capacity == cIndex)
        {
//            System.out.println(Thread.currentThread().getName()+"channelworker返回无法接受");
            return false;
        }
        else
        {
            return true;
        }
    }

    /**
     * 写处理器通知当前发送队列有可供写出的容量。
     */
    public void notifyedWriteAvailable()
    {
        recoverFromBlock();
    }

    private void recoverFromBlock()
    {
        int now = state;
        if (now == blockByDownStream && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, blockByDownStream, WORK))
        {
//            System.out.println(Thread.currentThread().getName() + "恢复成功");
            executorService.execute(this);
        }
        else
        {
//            System.out.println(Thread.currentThread().getName() + "恢复失败");
        }
    }

    private void tryExecute()
    {
        int now;
        if ((now = state) == IDLE && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
        {
            executorService.execute(this);
//            System.out.println(Thread.currentThread().getName() + "夺取成功");
        }
        else
        {
//            System.out.println(Thread.currentThread().getName() + "夺取失败，放弃");
        }
    }

    public void setUpStream(DataProcessor<?> upStream)
    {
        this.upStream = upStream;
    }

    public void setDownStream(DataProcessor downStream)
    {
        this.downStream = downStream;
    }

    class ChannelAttachEntity
    {
        ChannelContext   channelContext;
        DataProcessor<?> downStream;
        Object           data;

        void clear()
        {
            channelContext = null;
            downStream = null;
            data = null;
        }
    }

    public void setChannelContext(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }
}
