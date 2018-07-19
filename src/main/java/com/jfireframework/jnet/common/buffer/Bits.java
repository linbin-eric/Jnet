package com.jfireframework.jnet.common.buffer;

import java.lang.reflect.Method;
import java.nio.ByteOrder;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import com.jfireframework.baseutil.reflect.UNSAFE;

public final class Bits
{
	public static final int			JNI_COPY_TO_ARRAY_THRESHOLD		= 6;
	public static final int			JNI_COPY_FROM_ARRAY_THRESHOLD	= 6;
	private static final long		UNSAFE_COPY_THRESHOLD			= 1024L * 1024L;
	private static final int		arrayBaseOffset					= UNSAFE.arrayBaseOffset(byte[].class);
	public static boolean			unaligned;
	private static final boolean	nativeByteOrder;
	static
	{
		unaligned = detectUnaligned();
		nativeByteOrder = detectNativeByteOrder();
	}
	
	private static boolean detectNativeByteOrder()
	{
		try
		{
			Method method = Class.forName("java.nio.Bits").getDeclaredMethod("byteOrder");
			method.setAccessible(true);
			ByteOrder byteOrder = (ByteOrder) method.invoke(null);
			return byteOrder == ByteOrder.BIG_ENDIAN;
		}
		catch (Exception e)
		{
			ReflectUtil.throwException(e);
			return false;
		}
	}
	
	private static Boolean detectUnaligned()
	{
		try
		{
			if (System.getProperty("io.jnet.buffer.unaligned") != null)
			{
				return Boolean.valueOf(System.getProperty("io.jnet.buffer.unaligned"));
			}
			Method method = Class.forName("java.nio.Bits").getDeclaredMethod("unaligned");
			method.setAccessible(true);
			return (Boolean) method.invoke(null);
		}
		catch (Exception e)
		{
			ReflectUtil.throwException(e);
			return false;
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
			UNSAFE.copyMemory(src, offset, null, dstAddr, size);
			length -= size;
			offset += size;
			dstAddr += size;
		}
	}
	
	/**
	 * 从堆外内存拷贝字节数据到堆内内存
	 * 
	 * @param srcAddr
	 * @param dst
	 * @param dstPos
	 * @param length
	 */
	public static void copyToArray(long srcAddr, byte[] dst, int dstPos, long length)
	{
		long offset = arrayBaseOffset + dstPos;
		while (length > 0)
		{
			long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : length;
			UNSAFE.copyMemory(null, srcAddr, dst, offset, size);
			length -= size;
			srcAddr += size;
			offset += size;
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
		UNSAFE.copyMemory(srcAddr, destAddr, length);
	}
	
	public static void put(long address, byte b)
	{
		UNSAFE.putByte(address, b);
	}
	
	public static byte get(long address)
	{
		return UNSAFE.getByte(address);
	}
	
	public static void putInt(long address, int value)
	{
		if (unaligned)
		{
			UNSAFE.putInt(address, nativeByteOrder ? value : swap(value));
		}
		else
		{
			putIntBigendian(address, value);
		}
	}
	
	static void putIntBigendian(long address, int value)
	{
		put(address, int3(value));
		put(address + 1, int2(value));
		put(address + 2, int1(value));
		put(address + 3, int0(value));
	}
	
	public static int getInt(long address)
	{
		if (unaligned)
		{
			int x = UNSAFE.getInt(address);
			return nativeByteOrder ? x : swap(x);
		}
		return getIntBigendian(address);
	}
	
	static int getIntBigendian(long address)
	{
		return makeInt(get(address), get(address + 1), get(address + 2), get(address + 3));
	}
	
	public static void putLong(long address, long value)
	{
		if (unaligned)
		{
			UNSAFE.putLong(address, nativeByteOrder ? value : swap(value));
		}
		else
		{
			putLongBigEndian(address, value);
		}
	}
	
	static void putLongBigEndian(long address, long value)
	{
		put(address, long7(value));
		put(address + 1, long6(value));
		put(address + 2, long5(value));
		put(address + 3, long4(value));
		put(address + 4, long3(value));
		put(address + 5, long2(value));
		put(address + 6, long1(value));
		put(address + 7, long0(value));
	}
	
	public static long getLong(long address)
	{
		if (unaligned)
		{
			long value = UNSAFE.getLong(address);
			return nativeByteOrder ? value : swap(value);
		}
		return getLongBigEndian(address);
	}
	
	static long getLongBigEndian(long address)
	{
		return makeLong(get(address), //
		        get(address + 1), //
		        get(address + 2), //
		        get(address + 3), //
		        get(address + 4), //
		        get(address + 5), //
		        get(address + 6), //
		        get(address + 7));//
	}
	
	public static void putShort(long address, short s)
	{
		if (unaligned)
		{
			UNSAFE.putShort(address, nativeByteOrder ? s : swap(s));
		}
		else
		{
			putShortBigEndian(address, s);
		}
	}
	
	static void putShortBigEndian(long address, short s)
	{
		put(address, short1(s));
		put(address + 1, short0(s));
	}
	
	public static short getShort(long address)
	{
		if (unaligned)
		{
			short s = UNSAFE.getShort(address);
			return nativeByteOrder ? s : swap(s);
		}
		else
		{
			return makeShort(get(address), get(address + 1));
		}
	}
	
	public static void putInt(byte[] array, int posi, int value)
	{
		array[posi] = int3(value);
		array[posi + 1] = int2(value);
		array[posi + 2] = int1(value);
		array[posi + 3] = int0(value);
	}
	
	public static int getInt(byte[] array, int posi)
	{
		return makeInt(array[posi], //
		        array[posi + 1], //
		        array[posi + 2], //
		        array[posi + 3]);//
	}
	
	public static void putLong(byte[] array, int posi, long value)
	{
		array[posi] = long7(value);
		array[posi + 1] = long6(value);
		array[posi + 2] = long5(value);
		array[posi + 3] = long4(value);
		array[posi + 4] = long3(value);
		array[posi + 5] = long2(value);
		array[posi + 6] = long1(value);
		array[posi + 7] = long0(value);
	}
	
	public static long getLong(byte[] array, int posi)
	{
		return makeLong(array[posi], //
		        array[posi + 1], //
		        array[posi + 2], //
		        array[posi + 3], //
		        array[posi + 4], //
		        array[posi + 5], //
		        array[posi + 6], //
		        array[posi + 7]); //
	}
	
	public static void putShort(byte[] array, int posi, short s)
	{
		array[posi] = short1(s);
		array[posi + 1] = short0(s);
	}
	
	public static short getShort(byte[] array, int posi)
	{
		return makeShort(array[posi], array[posi + 1]);
	}
	
	static byte int3(int x)
	{
		return (byte) (x >> 24);
	}
	
	static byte int2(int x)
	{
		return (byte) (x >> 16);
	}
	
	static byte int1(int x)
	{
		return (byte) (x >> 8);
	}
	
	static byte int0(int x)
	{
		return (byte) (x);
	}
	
	static byte short1(short x)
	{
		return (byte) (x >> 8);
	}
	
	static byte short0(short x)
	{
		return (byte) (x);
	}
	
	static byte long7(long x)
	{
		return (byte) (x >> 56);
	}
	
	static byte long6(long x)
	{
		return (byte) (x >> 48);
	}
	
	static byte long5(long x)
	{
		return (byte) (x >> 40);
	}
	
	static byte long4(long x)
	{
		return (byte) (x >> 32);
	}
	
	static byte long3(long x)
	{
		return (byte) (x >> 24);
	}
	
	static byte long2(long x)
	{
		return (byte) (x >> 16);
	}
	
	static byte long1(long x)
	{
		return (byte) (x >> 8);
	}
	
	static byte long0(long x)
	{
		return (byte) (x);
	}
	
	static int makeInt(byte b3, byte b2, byte b1, byte b0)
	{
		return (((b3) << 24) | ((b2 & 0xff) << 16) | ((b1 & 0xff) << 8) | ((b0 & 0xff)));
	}
	
	static short makeShort(byte b1, byte b0)
	{
		return (short) ((b1 << 8) | (b0 & 0xff));
	}
	
	static long makeLong(byte b7, byte b6, byte b5, byte b4, byte b3, byte b2, byte b1, byte b0)
	{
		return ((((long) b7) << 56) | (((long) b6 & 0xff) << 48) | (((long) b5 & 0xff) << 40) | (((long) b4 & 0xff) << 32) | (((long) b3 & 0xff) << 24) | (((long) b2 & 0xff) << 16) | (((long) b1 & 0xff) << 8) | (((long) b0 & 0xff)));
	}
	
	static int swap(int x)
	{
		return Integer.reverseBytes(x);
	}
	
	static long swap(long x)
	{
		return Long.reverseBytes(x);
	}
	
	static short swap(short x)
	{
		return Short.reverseBytes(x);
	}
}
