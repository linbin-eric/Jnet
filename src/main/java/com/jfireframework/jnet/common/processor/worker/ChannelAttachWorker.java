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

    /**
     * 使用三个状态用来进行竞争。通过从IDLE状态到WORK状态，竞争将worker放入线程池执行的权利。
     * 当worker发送数据到下游被拒绝时，将状态切换到blockByDownStream。此时上游无法再次尝试启动worker。
     * 这种情况下，worker只能通过自我尝试恢复，或者被下游的可写信号唤醒，尝试从blockByDownStream状态切换到WORK状态，成功后启动worker。
     */
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
                            upStream.notifyedWriterAvailable();
                            if (queue.isEmpty() == false)
                            {
                                tryExecute();
                            }
                            else
                            {
                                ;
                            }
                            return;
                        }
                    }
                }
                if (downStream.process(avail) == false)
                {
                    state = blockByDownStream;
                    if (downStream.canAccept())
                    {
                        recoverFromBlock();
                    }
                    else
                    {
                        ;
                    }
                    break;
                }
            } while (true);
        } catch (Throwable e)
        {
            e.printStackTrace();
            channelContext.close(e);
        }
    }

    public boolean commit(ChannelContext channelContext, DataProcessor<?> downStream, Object data)
    {
        if (queue.offer((IoBuffer) data))
        {
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
            executorService.execute(this);
        }
        else
        {
            ;
        }
    }

    private void tryExecute()
    {
        int now = state;
        if (now == IDLE && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
        {
            executorService.execute(this);
        }
        else
        {
            ;
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

    public void setChannelContext(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
    }
}
