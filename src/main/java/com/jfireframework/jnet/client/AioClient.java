package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.buffer.AbstractIoBuffer;

public interface AioClient
{
    
    public void connect();
    
    public void close();
    
    public void write(AbstractIoBuffer packet);
    
}
