package com.jfireframework.jnet.common.buffer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.jnet.common.util.PlatFormFunction;
import sun.misc.Cleaner;

@SuppressWarnings("restriction")
public class DirectArena extends Arena<ByteBuffer>
{
	private static Field cleanerField;
	static
	{
		Class<?> directByteBufferClass = ByteBuffer.allocateDirect(1).getClass();
		try
		{
			cleanerField = directByteBufferClass.getDeclaredField("cleaner");
			cleanerField.setAccessible(true);
		}
		catch (NoSuchFieldException | SecurityException e)
		{
			ReflectUtil.throwException(e);
			cleanerField = null;
		}
	}
	
	public DirectArena(PooledBufferAllocator parent, int maxLevel, int pageSize, int pageSizeShift, int subpageOverflowMask)
	{
		super(parent, maxLevel, pageSize, pageSizeShift, subpageOverflowMask);
	}
	
	@Override
	Chunk<ByteBuffer> newChunk(int maxLevel, int pageSize, int pageSizeShift, int chunkSize)
	{
		return new DirectChunk(maxLevel, pageSize, pageSizeShift, chunkSize);
	}
	
	@Override
	Chunk<ByteBuffer> newChunk(int reqCapacity)
	{
		return new DirectChunk(reqCapacity);
	}
	
	@Override
	void destoryChunk(Chunk<ByteBuffer> chunk)
	{
		try
		{
			sun.misc.Cleaner cleaner = (Cleaner) cleanerField.get(chunk.memory);
			cleaner.clean();
		}
		catch (IllegalArgumentException | IllegalAccessException e)
		{
			ReflectUtil.throwException(e);
		}
	}
	
	@Override
	public boolean isDirect()
	{
		return true;
	}
	
	@Override
	void memoryCopy(ByteBuffer src, int srcOffset, ByteBuffer desc, int destOffset, int posi, int len)
	{
		long srcAddress = PlatFormFunction.bytebufferOffsetAddress(src) + srcOffset;
		long destAddress = PlatFormFunction.bytebufferOffsetAddress(desc) + destOffset;
		Bits.copyDirectMemory(srcAddress, destAddress, len);
	}
	
}
