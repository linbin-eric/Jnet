package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.DataProcessor;

public class ChainUtil
{
    public static ProcessorChain parse(final DataProcessor<?>[] processors, final ChannelContext channelContext)
    {
        abstract class InternelProcessorChain implements ProcessorChain
        {
            protected final InternelProcessorChain        next;
            protected final DataProcessor<? super Object> processor;
            
            @SuppressWarnings("unchecked")
            public InternelProcessorChain(InternelProcessorChain next, DataProcessor<?> processor)
            {
                this.next = next;
                this.processor = (DataProcessor<? super Object>) processor;
            }
            
            public abstract void execCurrent(Object data) throws Throwable;
            
        }
        InternelProcessorChain last = new InternelProcessorChain(null, null) {
            
            @Override
            public void chain(Object data)
            {
                
            }
            
            @Override
            public void execCurrent(Object data)
            {
                throw new NullPointerException("没有下一个节点了");
            }
        };
        for (int i = processors.length - 1; i > -1; i--)
        {
            InternelProcessorChain nextNode = last;
            last = new InternelProcessorChain(nextNode, processors[i]) {
                
                @Override
                public void chain(Object data) throws Throwable
                {
                    next.execCurrent(data);
                }
                
                @Override
                public void execCurrent(Object data) throws Throwable
                {
                    processor.process(data, this, channelContext);
                }
            };
        }
        InternelProcessorChain result = new InternelProcessorChain(last, null) {
            
            @Override
            public void chain(Object data) throws Throwable
            {
                next.execCurrent(data);
            }
            
            @Override
            public void execCurrent(Object data)
            {
                throw new NullPointerException();
            }
        };
        return result;
    }
}
