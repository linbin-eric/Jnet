package com.jfireframework.jnet.common.buffer;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;

class DirectChunk extends Chunk
{
	public static final Field addressField;
	static
	{
		try
		{
			addressField = Buffer.class.getDeclaredField("adddress");
			addressField.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	public static final long getAddress(ByteBuffer buffer)
	{
		try
		{
			return addressField.getLong(buffer);
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}
	
	private ByteBuffer buffer;
	
	public DirectChunk(int maxLevel, int unit)
	{
		super(maxLevel, unit);
	}
	
	@Override
	protected void initializeMem(int capacity)
	{
		buffer = ByteBuffer.allocateDirect(capacity);
		address = getAddress(buffer);
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	@Override
	protected void initBuffer(PooledIoBuffer buffer, int index, int off, int capacity)
	{
		buffer.setDirectIoBufferArgs(archon, this, index, address, off, capacity);
	}
	
}
