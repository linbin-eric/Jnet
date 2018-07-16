package com.jfireframework.jnet.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.Future;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.api.AioListener;
import com.jfireframework.jnet.common.api.ChannelContext;
import com.jfireframework.jnet.common.api.ChannelContextInitializer;
import com.jfireframework.jnet.common.api.ReadCompletionHandler;
import com.jfireframework.jnet.common.api.WriteCompletionHandler;
import com.jfireframework.jnet.common.buffer.BufferAllocator;
import com.jfireframework.jnet.common.buffer.IoBuffer;
import com.jfireframework.jnet.common.internal.DefaultChannelContext;
import com.jfireframework.jnet.common.internal.DefaultReadCompletionHandler;
import com.jfireframework.jnet.common.internal.SingleWriteCompletionHandler;

public class DefaultClient implements AioClient
{
	private ChannelContextInitializer	channelContextInitializer;
	private final String				ip;
	private final int					port;
	private final AioListener			aioListener;
	private final BufferAllocator		allocator;
	private ChannelContext				channelContext;
	private int							state;
	private static final int			NOT_INIT		= 1;
	private static final int			CONNECTED		= 2;
	private static final int			DISCONNECTED	= 3;
	
	public DefaultClient(ChannelContextInitializer channelContextInitializer, String ip, int port, AioListener aioListener, BufferAllocator allocator)
	{
		this.channelContextInitializer = channelContextInitializer;
		this.ip = ip;
		this.port = port;
		this.aioListener = aioListener;
		this.allocator = allocator;
	}
	
	@Override
	public void write(IoBuffer packet) throws Exception
	{
		if (state == NOT_INIT || state == DISCONNECTED)
		{
			try
			{
				AsynchronousSocketChannel asynchronousSocketChannel = AsynchronousSocketChannel.open();
				Future<Void> future = asynchronousSocketChannel.connect(new InetSocketAddress(ip, port));
				future.get();
				channelContext = new DefaultChannelContext(asynchronousSocketChannel, aioListener);
				channelContextInitializer.onChannelContextInit(channelContext);
				ReadCompletionHandler readCompletionHandler = new DefaultReadCompletionHandler(aioListener, allocator, asynchronousSocketChannel);
				readCompletionHandler.bind(channelContext);
				WriteCompletionHandler writeCompletionHandler = new SingleWriteCompletionHandler(asynchronousSocketChannel, aioListener, allocator, 512);
				channelContext.bindWriteCompleteHandler(writeCompletionHandler);
				readCompletionHandler.start();
				state = CONNECTED;
			}
			catch (Exception e)
			{
				ReflectUtil.throwException(e);
				return;
			}
		}
		try
		{
			channelContext.write(packet);
		}
		catch (Throwable e)
		{
			state = DISCONNECTED;
			ReflectUtil.throwException(e);
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
}
