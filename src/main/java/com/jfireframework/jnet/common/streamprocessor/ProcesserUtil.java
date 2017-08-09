package com.jfireframework.jnet.common.streamprocessor;

import com.jfireframework.jnet.common.api.ChannelContext;

public class ProcesserUtil
{
    public static Object process(ChannelContext context, StreamProcessor[] processors, Object target, int index) throws Throwable
    {
        ProcessorIndexFlag streamResult = new ProcessorIndexFlag();
        streamResult.setIndex(index);
        Object intermediateResult = target;
        for (int i = index; i < processors.length;)
        {
            intermediateResult = processors[i].process(intermediateResult, streamResult, context);
            if (i == streamResult.getIndex())
            {
                i++;
                streamResult.setIndex(i);
            }
            else
            {
                i = streamResult.getIndex();
            }
        }
        return intermediateResult;
    }
    
}
