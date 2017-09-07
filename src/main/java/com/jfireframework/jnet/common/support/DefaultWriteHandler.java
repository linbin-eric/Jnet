package com.jfireframework.jnet.common.support;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.baseutil.collection.buffer.ByteBuf;
import com.jfireframework.baseutil.concurrent.CpuCachePadingInt;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.WriteHandler;
import com.jfireframework.jnet.common.bufstorage.SendBufStorage;
import com.jfireframework.jnet.common.util.ByteBufFactory;

public class DefaultWriteHandler implements WriteHandler
{
	private static final int				SPIN_THRESHOLD		= 1 << 7;
	private final static int				WORK				= 1;
	private final static int				IDLE				= 2;
	private final ByteBuf<?>				outCachedBuf;
	private final ByteBuf<?>[]				bufArray;
	private final CpuCachePadingInt			status				= new CpuCachePadingInt(IDLE);
	private final ChannelContext			channelContext;
	private final AsynchronousSocketChannel	socketChannel;
	private final AioListener				aioListener;
	private final SendBufStorage			bufStorage;
	private int								currentSendCount	= 0;
	
	public DefaultWriteHandler(AioListener aioListener, ChannelContext serverChannelContext)
	{
		this.aioListener = aioListener;
		this.channelContext = serverChannelContext;
		bufArray = new ByteBuf<?>[serverChannelContext.maxMerge()];
		outCachedBuf = serverChannelContext.outCachedBuf();
		socketChannel = serverChannelContext.socketChannel();
		bufStorage = serverChannelContext.sendBufStorage();
	}
	
	@Override
	public void completed(Integer result, ByteBuf<?> buf)
	{
		ByteBuffer buffer = buf.cachedNioBuffer();
		if (buffer.hasRemaining())
		{
			socketChannel.write(buffer, buf, this);
			return;
		}
		buf.clear();
		aioListener.afterWrited(channelContext, currentSendCount);
		writeNextBuf();
	}
	
	private void writeNextBuf()
	{
		currentSendCount = bufStorage.batchNext(bufArray, bufArray.length);
		if (currentSendCount != 0)
		{
			commitWrite();
		}
		else
		{
			for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
			{
				if (bufStorage.isEmpty() == false)
				{
					currentSendCount = bufStorage.batchNext(bufArray, bufArray.length);
					commitWrite();
					return;
				}
			}
			status.set(IDLE);
			if (bufStorage.isEmpty() == false)
			{
				registerWrite();
			}
		}
	}
	
	private void commitWrite()
	{
		for (int i = 0; i < currentSendCount; i++)
		{
			outCachedBuf.put(bufArray[i]);
			ByteBufFactory.release(bufArray[i]);
			bufArray[i] = null;
		}
		socketChannel.write(outCachedBuf.nioBuffer(), outCachedBuf, this);
	}
	
	@Override
	public void failed(Throwable exc, ByteBuf<?> buf)
	{
		ByteBufFactory.release(buf);
		try
		{
			aioListener.catchException(exc, channelContext);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (channelContext.isOpen() == false)
			{
				do
				{
					buf = bufStorage.next();
					if (buf != null)
					{
						ByteBufFactory.release(buf);
					}
					else
					{
						break;
					}
				} while (true);
			}
		}
	}
	
	public void registerWrite()
	{
		int now = status.value();
		if (now == IDLE && status.compareAndSwap(IDLE, WORK))
		{
			writeNextBuf();
		}
	}
}
