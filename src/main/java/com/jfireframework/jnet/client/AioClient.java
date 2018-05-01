package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

public interface AioClient
{
    
    public void connect();
    
    public void close();
    
    public void write(PooledIoBuffer packet);
    
}
