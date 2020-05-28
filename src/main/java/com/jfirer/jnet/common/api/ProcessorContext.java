package com.jfirer.jnet.common.api;

public interface ProcessorContext
{
    void fireRead(Object data);

    void fireWrite(Object data);

    JnetWorker worker();

    Pipeline pipeline();
}
