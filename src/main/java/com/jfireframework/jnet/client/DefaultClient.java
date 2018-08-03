package com.jfireframework.jnet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.BackPressureMode;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;
import com.jfireframework.jnet.common.internal.DefaultWriteCompleteHandler;

public class DefaultClient implements JnetClient
{
	private static final int				NOT_INIT		= 1;
	private static final int				CONNECTED		= 2;
	private static final int				DISCONNECTED	= 3;
	private ChannelContextInitializer		channelContextInitializer;
	private final String					ip;
	private final int						port;
	private final AioListener				aioListener;
	private final BufferAllocator			allocator;
	private final boolean					preferBlock		= false;
	private final AsynchronousChannelGroup	channelGroup;
	private ChannelContext					channelContext;
	private int								state			= NOT_INIT;
	private BackPressureMode				backPressureMode;
	
	public DefaultClient(ChannelContextInitializer channelContextInitializer, String ip, int port, AioListener aioListener, BufferAllocator allocator, AsynchronousChannelGroup channelGroup, BackPressureMode backPressureMode)
	{
		this.channelContextInitializer = channelContextInitializer;
		this.ip = ip;
		this.port = port;
		this.aioListener = aioListener;
		this.allocator = allocator;
		this.channelGroup = channelGroup;
		this.backPressureMode = backPressureMode;
	}
	
	@Override
	public void write(IoBuffer packet) throws Exception
	{
		write(packet, preferBlock);
	}
	
	private void connectIfNecessary()
	{
		if (state == NOT_INIT || state == DISCONNECTED)
		{
			try
			{
				AsynchronousSocketChannel asynchronousSocketChannel = channelGroup == null ? AsynchronousSocketChannel.open() : AsynchronousSocketChannel.open(channelGroup);
				Future<Void> future = asynchronousSocketChannel.connect(new InetSocketAddress(ip, port));
				future.get();
				channelContext = new DefaultChannelContext(asynchronousSocketChannel, aioListener, null);
				channelContextInitializer.onChannelContextInit(channelContext);
				ReadCompletionHandler readCompletionHandler = new DefaultReadCompletionHandler(aioListener, allocator, asynchronousSocketChannel);
				readCompletionHandler.bind(channelContext);
				WriteCompletionHandler writeCompletionHandler = new DefaultWriteCompleteHandler(asynchronousSocketChannel, aioListener, allocator, 1024 * 1024 * 2, backPressureMode);
				channelContext.bind(writeCompletionHandler, readCompletionHandler);
				readCompletionHandler.start();
				state = CONNECTED;
			}
			catch (Exception e)
			{
				ReflectUtil.throwException(e);
				return;
			}
		}
	}
	
	void blockWrite(IoBuffer buffer)
	{
		ByteBuffer readableByteBuffer = buffer.readableByteBuffer();
		while (readableByteBuffer.hasRemaining())
		{
			try
			{
				channelContext.socketChannel().write(readableByteBuffer).get();
			}
			catch (Throwable e)
			{
				close();
				ReflectUtil.throwException(e);
			}
		}
		buffer.free();
	}
	
	void nonBlockWrite(IoBuffer buffer)
	{
		while (channelContext.write(buffer) == false)
		{
			Thread.yield();
		}
	}
	
	@Override
	public void close()
	{
		if (state == NOT_INIT || state == DISCONNECTED)
		{
			return;
		}
		else
		{
			state = DISCONNECTED;
			try
			{
				channelContext.socketChannel().close();
			}
			catch (IOException e)
			{
				ReflectUtil.throwException(e);
			}
		}
	}
	
	@Override
	public void write(IoBuffer packet, boolean block) throws Exception
	{
		connectIfNecessary();
		if (block)
		{
			blockWrite(packet);
		}
		else
		{
			nonBlockWrite(packet);
		}
	}
	
	@Override
	public boolean preferBlock()
	{
		return preferBlock;
	}
}
