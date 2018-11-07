package com.jfireframework.jnet.common.processor.worker;

import com.jfireframework.baseutil.concurrent.FastMPSCArrayQueue;
import com.jfireframework.baseutil.reflect.UNSAFE;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.DataProcessor;
import com.jfireframework.jnet.common.util.FixArray;
import com.jfireframework.jnet.common.util.MPSCFixArray;
import com.jfireframework.jnet.common.util.SPSCFixArray;

import java.util.concurrent.ExecutorService;

public class ChannelAttachWorker implements Runnable
{

    private static final int                           IDLE           = -1;
    private static final int                           WORK           = 1;
    private static final int                           SPIN_THRESHOLD = 128;
    private static final long                          STATE_OFFSET   = UNSAFE.getFieldOffset("state", ChannelAttachWorker.class);
    private final        ExecutorService               executorService;
    private final        FixArray<ChannelAttachEntity> entities       = new SPSCFixArray<ChannelAttachEntity>(512)
    {

        @Override
        protected ChannelAttachEntity newInstance()
        {
            return new ChannelAttachEntity();
        }
    };
    private volatile     int                           state          = IDLE;
    private              DataProcessor<?>              upStream;
    private              DataProcessor<?>              downStream;
    private              ChannelContext                channelContext;

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
                long avail = entities.nextAvail();
                if (avail == -1)
                {
                    spin = 0;
                    for (; ; )
                    {
                        if ((avail = entities.nextAvail()) != -1)
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
                            upStream.notifyedWriteAvailable();
                            if (entities.isEmpty() == false)
                            {
                                tryExecute();
                            }
                            return;
                        }
                    }
                }
                ChannelAttachEntity slot           = entities.getSlot(avail);
                ChannelContext      channelContext = slot.channelContext;
                DataProcessor       downStream     = slot.downStream;
                Object              data           = slot.data;
                slot.clear();
                entities.comsumeAvail(avail);
                if (downStream.process(data) == false)
                {
                    state = IDLE;
                    tryExecute();
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
        long offerIndexAvail = entities.nextOfferIndex();
        if (offerIndexAvail == -1)
        {
            return false;
        }
        ChannelAttachEntity slot = entities.getSlot(offerIndexAvail);
        slot.channelContext = channelContext;
        slot.downStream = downStream;
        slot.data = data;
        entities.commit(offerIndexAvail);
        tryExecute();
        return true;
    }

    public boolean canAccept()
    {
        long offerIndexAvail = entities.nextOfferIndex();
        if (offerIndexAvail == -1)
        {
            return false;
        }
        return true;
    }

    /**
     * 写处理器通知当前发送队列有可供写出的容量。
     */
    public void notifyedWriteAvailable()
    {
        tryExecute();
    }

    private void tryExecute()
    {
        int now;
        if (downStream.canAccept() && (now = state) == IDLE && UNSAFE.compareAndSwapInt(this, STATE_OFFSET, IDLE, WORK))
        {
            executorService.execute(this);
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
