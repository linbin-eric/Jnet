package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import com.jfireframework.jnet.common.recycler.Recycler;
import com.jfireframework.jnet.common.recycler.Recycler.RecycleHandler;

public class ThreadPooledIoBufferAllocator implements IoBufferAllocator
{
	private static final Recycler<IoBuffer>	LOCALHEAP	= new Recycler<IoBuffer>() {
															
															@Override
															protected IoBuffer newObject(RecycleHandler handler)
															{
																IoBufferRecycleHandlerProxy proxy = new IoBufferRecycleHandlerProxy();
																proxy.handler = handler;
																proxy.buffer = IoBuffer.heapIoBuffer();
																return proxy;
															}
															
														};
	private static final Recycler<IoBuffer>	LOCALDIRECT	= new Recycler<IoBuffer>() {
															
															@Override
															protected IoBuffer newObject(RecycleHandler handler)
															{
																IoBufferRecycleHandlerProxy proxy = new IoBufferRecycleHandlerProxy();
																proxy.handler = handler;
																proxy.buffer = IoBuffer.directBuffer();
																return proxy;
															}
															
														};
	
	private Archon[]						heapArchons;
	private Archon[]						directArchons;
	
	private AtomicInteger					idx			= new AtomicInteger(0);
	private final int						mask;
	
	public ThreadPooledIoBufferAllocator()
	{
		mask = (1 << 3) - 1;
		heapArchons = new Archon[mask + 1];
		for (int i = 0; i < heapArchons.length; i++)
		{
			heapArchons[i] = PooledArchon.heapPooledArchon(8, 128);
		}
		directArchons = new Archon[mask + 1];
		for (int i = 0; i < directArchons.length; i++)
		{
			directArchons[i] = PooledArchon.directPooledArchon(8, 128);
		}
	}
	
	@Override
	public IoBuffer allocate(int initSize)
	{
		int index = idx.incrementAndGet() & mask;
		IoBuffer buffer = LOCALHEAP.get();
		if (buffer.archon == null)
		{
			heapArchons[index].apply(initSize, buffer);
		}
		return buffer;
	}
	
	@Override
	public IoBuffer allocateDirect(int initSize)
	{
		int index = idx.incrementAndGet() & mask;
		IoBuffer buffer = LOCALDIRECT.get();
		if (buffer.archon == null)
		{
			directArchons[index].apply(initSize, buffer);
		}
		return buffer;
	}
	
	@Override
	public void release(IoBuffer ioBuffer)
	{
		((IoBufferRecycleHandlerProxy) ioBuffer).handler.recycle(ioBuffer);
	}
	
	static class IoBufferRecycleHandlerProxy extends IoBuffer implements RecycleHandler
	{
		private RecycleHandler	handler;
		private IoBuffer		buffer;
		
		@Override
		public void recycle(Object item)
		{
			handler.recycle(item);
		}
		
		@Override
		protected void _initialize(int off, int capacity, Object mem)
		{
			buffer._initialize(off, capacity, mem);
		}
		
		@Override
		protected void expansion(IoBuffer src)
		{
			if (src instanceof IoBufferRecycleHandlerProxy)
			{
				buffer.expansion(((IoBufferRecycleHandlerProxy) src).buffer);
			}
			else
			{
				buffer.expansion(src);
			}
		}
		
		@Override
		protected void _destoryMem()
		{
			buffer._destoryMem();
		}
		
		@Override
		protected void _put(byte b)
		{
			buffer._put(b);
		}
		
		@Override
		protected void _put(byte b, int posi)
		{
			buffer._put(b, posi);
		}
		
		@Override
		protected void _put(byte[] content)
		{
			buffer._put(content);
		}
		
		@Override
		protected void _put(byte[] content, int off, int len)
		{
			buffer._put(content, off, len);
		}
		
		@Override
		protected void _put(IoBuffer src, int len)
		{
			if (src instanceof IoBufferRecycleHandlerProxy)
			{
				buffer._put(((IoBufferRecycleHandlerProxy) src).buffer, len);
			}
			else
			{
				buffer._put(src, len);
			}
		}
		
		@Override
		protected void _writeInt(int i, int off)
		{
			buffer._writeInt(i, off);
		}
		
		@Override
		protected void _writeShort(short s, int off)
		{
			buffer._writeShort(s, off);
		}
		
		@Override
		protected void _writeLong(long l, int off)
		{
			buffer._writeLong(l, off);
		}
		
		@Override
		protected void _writeInt(int i)
		{
			buffer._writeInt(i);
		}
		
		@Override
		protected void _writeShort(short s)
		{
			buffer._writeShort(s);
		}
		
		@Override
		protected void _writeLong(long l)
		{
			buffer._writeLong(l);
		}
		
		@Override
		public int getReadPosi()
		{
			return buffer.getReadPosi();
		}
		
		@Override
		public void setReadPosi(int readPosi)
		{
			buffer.setReadPosi(readPosi);
		}
		
		@Override
		public int getWritePosi()
		{
			return buffer.getWritePosi();
		}
		
		@Override
		public void setWritePosi(int writePosi)
		{
			buffer.setWritePosi(writePosi);
		}
		
		@Override
		public IoBuffer clearData()
		{
			return buffer.clearData();
		}
		
		@Override
		public byte get()
		{
			return buffer.get();
		}
		
		@Override
		public byte get(int posi)
		{
			return buffer.get(posi);
		}
		
		@Override
		public int remainRead()
		{
			return buffer.remainRead();
		}
		
		@Override
		public int remainWrite()
		{
			return buffer.remainWrite();
		}
		
		@Override
		public IoBuffer compact()
		{
			return buffer.compact();
		}
		
		@Override
		public IoBuffer get(byte[] content)
		{
			return buffer.get(content);
		}
		
		@Override
		public IoBuffer get(byte[] content, int off, int len)
		{
			return buffer.get(content, off, len);
		}
		
		@Override
		public void addReadPosi(int add)
		{
			buffer.addReadPosi(add);
		}
		
		@Override
		public void addWritePosi(int add)
		{
			buffer.addWritePosi(add);
		}
		
		@Override
		public int indexOf(byte[] array)
		{
			return buffer.indexOf(array);
		}
		
		@Override
		public int readInt()
		{
			return buffer.readInt();
		}
		
		@Override
		public short readShort()
		{
			return buffer.readShort();
		}
		
		@Override
		public long readLong()
		{
			return buffer.readLong();
		}
		
		@Override
		public ByteBuffer byteBuffer()
		{
			return buffer.byteBuffer();
		}
		
		@Override
		public boolean isDirect()
		{
			return buffer.isDirect();
		}
		
	}
}
