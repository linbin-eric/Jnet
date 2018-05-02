package com.jfireframework.jnet.common.util;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class Bits
{
	private static final Unsafe	unsafe					= ReflectUtil.getUnsafe();
	private static final long	UNSAFE_COPY_THRESHOLD	= 1024L * 1024L;
	private static final int	arrayBaseOffset			= unsafe.arrayBaseOffset(byte[].class);
	private static final Field	addressField;
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
	
	/**
	 * 从堆内存中拷贝数据到堆外内存
	 * 
	 * @param src 拷贝数据源
	 * @param srcPos 拷贝起始量
	 * @param dstAddr 堆外内存的地址
	 * @param length 拷贝长度
	 */
	public static void copyFromByteArray(byte[] src, int srcPos, long dstAddr, long length)
	{
		long offset = arrayBaseOffset + srcPos;
		while (length > 0)
		{
			long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : length;
			unsafe.copyMemory(src, offset, null, dstAddr, size);
			length -= size;
			offset += size;
			dstAddr += size;
		}
	}
	
	/**
	 * 在两个堆外内存中进行数据拷贝
	 * 
	 * @param srcAddr 拷贝源的堆外内存位置
	 * @param destAddr 拷贝目的的堆外内存位置
	 * @param length 拷贝长度
	 */
	public static void copyDirectMemory(long srcAddr, long destAddr, long length)
	{
		unsafe.copyMemory(srcAddr, destAddr, length);
	}
	
}
