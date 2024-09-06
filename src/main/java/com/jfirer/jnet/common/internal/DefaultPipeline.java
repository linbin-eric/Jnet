package com.jfirer.jnet.common.internal;

import com.jfirer.jnet.common.api.*;

public class DefaultPipeline implements InternalPipeline
{
    protected ChannelContext     channelContext;
    protected ReadProcessorNode  readHead;
    protected WriteProcessorNode writeHead;

    public DefaultPipeline(ChannelContext channelContext)
    {
        this.channelContext = channelContext;
        writeHead           = new WriteHead(channelContext.channelConfig().getWorkerGroup().next());
        readHead            = channelContext.channelConfig().isREAD_USE_CURRENT_THREAD() ? new ReadHeadUseCurrentThreaad(channelContext.pipeline()) : new ReadHeadUseWorker(channelContext.channelConfig().getWorkerGroup().next(), channelContext.pipeline());
    }

    @Override
    public void fireWrite(Object data)
    {
        writeHead.fireWrite(data);
    }

    @Override
    public void addReadProcessor(ReadProcessor<?> processor)
    {
        ReadProcessorNode node = readHead;
        while (node.next() != null)
        {
            node = node.next();
        }
        node.setNext(new ReadProcessorNodeImpl(processor, this));
    }

    @Override
    public void addWriteProcessor(WriteProcessor<?> processor)
    {
        WriteProcessorNode node = writeHead;
        while (node.next() != null)
        {
            node = node.next();
        }
        node.setNext(new WriteProcessorNodeImpl(processor));
    }

    @Override
    public ChannelContext channelContext()
    {
        return channelContext;
    }

    @Override
    public void startReadIO()
    {
        ReadCompletionHandler readCompletionHandler = new AdaptiveReadCompletionHandler(channelContext);
        readCompletionHandler.start();
    }

    @Override
    public void fireRead(Object data)
    {
        readHead.fireRead(data);
    }

    @Override
    public void fireChannelClose(Throwable e)
    {
        readHead.fireChannelClose(e);
    }

    @Override
    public void fireExceptionCatch(Throwable e)
    {
        readHead.fireExceptionCatch(e);
    }

    @Override
    public void complete()
    {
        addReadProcessor(ReadProcessor.TAIL);
        addWriteProcessor(new TailWriteProcessorImpl(channelContext));
        readHead.firePipelineComplete(this);
    }

    @Override
    public void fireReadClose()
    {
        readHead.fireReadClose();
    }

    @Override
    public void fireWriteClose()
    {
        writeHead.fireWriteClose();
    }

    class ReadHeadUseCurrentThreaad implements ReadProcessorNode
    {
        protected final Pipeline          pipeline;
        protected       ReadProcessorNode next;

        ReadHeadUseCurrentThreaad(Pipeline pipeline) {this.pipeline = pipeline;}

        @Override
        public void fireRead(Object data)
        {
            next.fireRead(data);
        }

        @Override
        public void firePipelineComplete(Pipeline pipeline)
        {
            next.firePipelineComplete(pipeline);
        }

        @Override
        public void fireExceptionCatch(Throwable e)
        {
            next.fireExceptionCatch(e);
        }

        @Override
        public void fireReadClose()
        {
            next.fireReadClose();
        }

        @Override
        public void fireChannelClose(Throwable e)
        {
            next.fireChannelClose(e);
        }

        @Override
        public void setNext(ReadProcessorNode next)
        {
            this.next = next;
        }

        @Override
        public ReadProcessorNode next()
        {
            return next;
        }

        @Override
        public Pipeline pipeline()
        {
            return pipeline;
        }
    }

    class ReadHeadUseWorker implements ReadProcessorNode
    {
        protected final JnetWorker        worker;
        protected final Pipeline          pipeline;
        private         ReadProcessorNode next;

        ReadHeadUseWorker(JnetWorker worker, Pipeline pipeline)
        {
            this.worker   = worker;
            this.pipeline = pipeline;
        }

        @Override
        public void fireRead(Object data)
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireRead(data);
            }
            else
            {
                worker.submit(() -> next.fireRead(data));
            }
        }

        @Override
        public void firePipelineComplete(Pipeline pipeline)
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.firePipelineComplete(pipeline);
            }
            else
            {
                worker.submit(() -> next.firePipelineComplete(pipeline));
            }
        }

        @Override
        public void fireExceptionCatch(Throwable e)
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireExceptionCatch(e);
            }
            else
            {
                worker.submit(() -> next.fireExceptionCatch(e));
            }
        }

        @Override
        public void fireReadClose()
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireReadClose();
            }
            else
            {
                worker.submit(() -> next.fireReadClose());
            }
        }

        @Override
        public void fireChannelClose(Throwable e)
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireChannelClose(e);
            }
            else
            {
                worker.submit(() -> next.fireChannelClose(e));
            }
        }

        @Override
        public void setNext(ReadProcessorNode next)
        {
            this.next = next;
        }

        @Override
        public ReadProcessorNode next()
        {
            return next;
        }

        @Override
        public Pipeline pipeline()
        {
            return pipeline;
        }
    }

    class WriteHead implements WriteProcessorNode
    {
        private final JnetWorker         worker;
        private       WriteProcessorNode next;

        public WriteHead(JnetWorker worker)
        {
            this.worker = worker;
        }

        @Override
        public void fireWrite(Object data)
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireWrite(data);
            }
            else
            {
                worker.submit(() -> next.fireWrite(data));
            }
        }

        @Override
        public void fireWriteClose()
        {
            if (Thread.currentThread() == worker.thread())
            {
                next.fireWriteClose();
            }
            else
            {
                worker.submit(() -> next.fireWriteClose());
            }
        }

        @Override
        public void setNext(WriteProcessorNode next)
        {
            this.next = next;
        }

        @Override
        public WriteProcessorNode next()
        {
            return next;
        }
    }
}
