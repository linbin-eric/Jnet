package com.jfireframework.jnet.client;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;

public interface AioClient
{
    
    public void connect();
    
    public void close();
    
    public void write(ByteBuf<?> packet);
    
}
