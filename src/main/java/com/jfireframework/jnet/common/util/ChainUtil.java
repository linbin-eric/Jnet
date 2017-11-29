package com.jfireframework.jnet.common.util;

import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ProcessorChain;
import com.jfireframework.jnet.common.api.ReadProcessor;

public class ChainUtil
{
    public static ProcessorChain demo(final ReadProcessor[] processors, final ChannelContext channelContext)
    {
        abstract class InternelProcessorChain implements ProcessorChain
        {
            public abstract void execCurrent(Object data);
            
        }
        
        InternelProcessorChain last = new InternelProcessorChain() {
            
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
            final int index = i;
            final InternelProcessorChain nextNode = last;
            last = new InternelProcessorChain() {
                
                @Override
                public void chain(Object data)
                {
                    nextNode.execCurrent(data);
                }
                
                @Override
                public void execCurrent(Object data)
                {
                    processors[index].process(data, this, channelContext);
                }
            };
        }
        final InternelProcessorChain first = last;
        InternelProcessorChain result = new InternelProcessorChain() {
            
            @Override
            public void chain(Object data)
            {
                first.execCurrent(data);
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
