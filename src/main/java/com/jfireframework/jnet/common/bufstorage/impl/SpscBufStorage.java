package com.jfireframework.jnet.common.bufstorage.impl;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.SpscQueue;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;

public class SpscBufStorage implements SendBufStorage
{
	private SpscQueue<ByteBuf<?>> storage = new SpscQueue<>();
	
	@Override
	public StorageType type()
	{
		return StorageType.spsc;
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
