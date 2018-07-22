package com.jfireframework.jnet.common.api;

public interface ProcessorInvoker
{
    void process(Object data) throws Throwable;
    
    boolean backpressureProcess(Object data) throws Throwable;
}
