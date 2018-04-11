package com.jfireframework.jnet.client;

import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface AioClient
{
    
    public void connect();
    
    public void close();
    
    public void write(IoBuffer packet);
    
}
