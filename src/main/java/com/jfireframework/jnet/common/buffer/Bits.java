package com.jfireframework.jnet.common.buffer;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import com.jfireframework.baseutil.reflect.ReflectUtil;
import sun.misc.Unsafe;

@SuppressWarnings("restriction")
public class Bits
{
    private static final Unsafe unsafe                        = ReflectUtil.getUnsafe();
    public static final int     JNI_COPY_TO_ARRAY_THRESHOLD   = 6;
    public static final int     JNI_COPY_FROM_ARRAY_THRESHOLD = 6;
    private static final long   UNSAFE_COPY_THRESHOLD         = 1024L * 1024L;
    private static final int    arrayBaseOffset               = unsafe.arrayBaseOffset(byte[].class);
    private static final Field  addressField;
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
    
    public static void copyToArray(long srcAddr, byte[] dst, int dstPos, long length)
    {
        long offset = arrayBaseOffset + dstPos;
        while (length > 0)
        {
            long size = (length > UNSAFE_COPY_THRESHOLD) ? UNSAFE_COPY_THRESHOLD : length;
            unsafe.copyMemory(null, srcAddr, dst, offset, size);
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
        unsafe.copyMemory(srcAddr, destAddr, length);
    }
    
    public static void putBytes(long address, byte b)
    {
        unsafe.putByte(address, b);
    }
    
    public static byte getBytes(long address)
    {
        return unsafe.getByte(address);
    }
    
    private static byte int3(int x)
    {
        return (byte) (x >> 24);
    }
    
    private static byte int2(int x)
    {
        return (byte) (x >> 16);
    }
    
    private static byte int1(int x)
    {
        return (byte) (x >> 8);
    }
    
    private static byte int0(int x)
    {
        return (byte) (x);
    }
    
    public static void putInt(AbstractIoBuffer buffer, int posi, int value)
    {
        buffer._put(posi, int3(value));
        buffer._put(posi + 1, int2(value));
        buffer._put(posi + 2, int1(value));
        buffer._put(posi + 3, int0(value));
    }
}
