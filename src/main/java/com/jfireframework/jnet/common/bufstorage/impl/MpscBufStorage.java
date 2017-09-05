package com.jfireframework.jnet.common.bufstorage.impl;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.MPSCQueue;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;

public class MpscBufStorage implements SendBufStorage
{
    private MPSCQueue<ByteBuf<?>> storage = new MPSCQueue<>();
    
    @Override
    public StorageType type()
    {
        return StorageType.mpsc;
    }
    
    @Override
    public boolean putBuf(ByteBuf<?> buf)
    {
        storage.offer(buf);
        return true;
    }
    
    @Override
    public ByteBuf<?> next()
    {
        return storage.poll();
    }
    
    @Override
    public int batchNext(ByteBuf<?>[] store, int max)
    {
        return storage.drain(store, max);
    }
    
    @Override
    public boolean isEmpty()
    {
        return storage.isEmpty();
    }
    
}
