package com.jfirer.jnet.common.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class UNSAFE
{
    private static final Unsafe  unsafe;
    private static final boolean hasUnsafe;

    static
    {
        Unsafe  un;
        boolean hasUnsafe1 = false;
        try
        {
            // 由反编译Unsafe类获得的信息
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            // 获取静态属性,Unsafe在启动JVM时随rt.jar装载
            un = (Unsafe) field.get(null);
            hasUnsafe1 = true;
        }
        catch (Exception e)
        {
            un = null;
            hasUnsafe1 = false;
        }
        unsafe = un;
        hasUnsafe = hasUnsafe1;
    }

    public static boolean isAvailable()
    {
        return hasUnsafe;
    }

    public static void throwThrowable(Throwable e)
    {
        unsafe.throwException(e);
    }

    /**
     * 获取字段的偏移量
     *
     * @param fieldName
     * @param type
     * @return
     */
    public static long getFieldOffset(String fieldName, Class<?> type)
    {
        try
        {
            Field field = type.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()))
            {
                throw new IllegalArgumentException();
            }
            return unsafe.objectFieldOffset(field);
        }
        catch (Exception e)
        {
            ReflectUtil.throwException(e);
            return 0;
        }
    }

    public static long getFieldOffset(String fieldName)
    {
        try
        {
            String className = Thread.currentThread().getStackTrace()[2].getClassName();
            Field  field     = Class.forName(className).getDeclaredField(fieldName);
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers()))
            {
                throw new IllegalArgumentException();
            }
            return unsafe.objectFieldOffset(field);
        }
        catch (Exception e)
        {
            ReflectUtil.throwException(e);
            return 0;
        }
    }

    public static long objectFieldOffset(Field field)
    {
        return unsafe.objectFieldOffset(field);
    }

    public static boolean compareAndSwapInt(Object src, long offset, int except, int newValue)
    {
        return unsafe.compareAndSwapInt(src, offset, except, newValue);
    }

    public static boolean compareAndSwapLong(Object src, long offset, long except, long newValue)
    {
        return unsafe.compareAndSwapLong(src, offset, except, newValue);
    }

    public static boolean compareAndSwapObject(Object src, long offset, Object except, Object newValue)
    {
        return unsafe.compareAndSwapObject(src, offset, except, newValue);
    }

    public static void putOrderedLong(Object src, long offset, long value)
    {
        unsafe.putOrderedLong(src, offset, value);
    }

    public static void putOrderedInt(Object src, long offset, int value)
    {
        unsafe.putOrderedInt(src, offset, value);
    }

    public static void putVolatileInt(Object src, long offset, int value)
    {
        unsafe.putIntVolatile(src, offset, value);
    }

    public static void putVolatileLong(Object src, long offset, long value)
    {
        unsafe.putLongVolatile(src, offset, value);
    }

    public static void putVolatileShort(Object src, long offset, short value)
    {
        unsafe.putShortVolatile(src, offset, value);
    }

    public static void putVolatileObject(Object src, long offset, Object value)
    {
        unsafe.putObjectVolatile(src, offset, value);
    }

    public static void putOrderedObject(Object src, long offset, Object value)
    {
        unsafe.putOrderedObject(src, offset, value);
    }

    public static int arrayBaseOffset(Class<?> ckass)
    {
        return unsafe.arrayBaseOffset(ckass);
    }

    public static int arrayIndexScale(Class<?> ckass)
    {
        return unsafe.arrayIndexScale(ckass);
    }

    public static int getIntVolatile(Object src, long offset)
    {
        return unsafe.getIntVolatile(src, offset);
    }

    public static long getLongVolatile(Object src, long offset)
    {
        return unsafe.getLongVolatile(src, offset);
    }

    public static Object getObjectVolatile(Object src, long offset)
    {
        return unsafe.getObjectVolatile(src, offset);
    }

    public static Object getObject(Object src, long offset)
    {
        return unsafe.getObject(src, offset);
    }

    public static void putObject(Object src, long offset, Object value)
    {
        unsafe.putObject(src, offset, value);
    }

    public static void putInt(Object src, long offset, int value)
    {
        unsafe.putInt(src, offset, value);
    }

    public static void putLong(Object src, long offset, long value)
    {
        unsafe.putLong(src, offset, value);
    }

    public static int getInt(Object src, long offset)
    {
        return unsafe.getInt(src, offset);
    }

    public static long getLong(Object src, long offset)
    {
        return unsafe.getLong(src, offset);
    }

    public static short getShort(Object src, long offset)
    {
        return unsafe.getShort(src, offset);
    }

    public static byte getByte(Object src, long offset)
    {
        return unsafe.getByte(src, offset);
    }

    public static char getChar(Object src, long offset)
    {
        return unsafe.getChar(src, offset);
    }

    public static float getFloat(Object src, long offset)
    {
        return unsafe.getFloat(src, offset);
    }

    public static double getDouble(Object src, long offset)
    {
        return unsafe.getDouble(src, offset);
    }

    public static boolean getBoolean(Object src, long offset)
    {
        return unsafe.getBoolean(src, offset);
    }

    public static void putByte(long address, byte b)
    {
        unsafe.putByte(address, b);
    }

    public static byte getByte(long addr)
    {
        return unsafe.getByte(addr);
    }

    public static void putInt(long addr, int i)
    {
        unsafe.putInt(addr, i);
    }

    public static int getInt(long addr)
    {
        return unsafe.getInt(addr);
    }

    public static void putLong(long addr, long value)
    {
        unsafe.putLong(addr, value);
    }

    public static long getLong(long addr)
    {
        return unsafe.getLong(addr);
    }

    public static void putShort(long addr, short s)
    {
        unsafe.putShort(addr, s);
    }

    public static short getShort(long addr)
    {
        return unsafe.getShort(addr);
    }

    public static void putShort(Object entity, long offset, short value)
    {
        unsafe.putShort(entity, offset, value);
    }

    public static void copyMemory(Object src, long srcOffset, Object desc, long descOffset, long len)
    {
        unsafe.copyMemory(src, srcOffset, desc, descOffset, len);
    }

    public static void copyMemory(long srcAddr, long destAddr, long len)
    {
        unsafe.copyMemory(srcAddr, destAddr, len);
    }

    public static void putChar(Object entity, long offset, char c)
    {
        unsafe.putChar(entity, offset, c);
    }

    public static void putByte(Object entity, long offset, byte b)
    {
        unsafe.putByte(entity, offset, b);
    }

    public static void putBoolean(Object entity, long offset, boolean b)
    {
        unsafe.putBoolean(entity, offset, b);
    }

    public static void putFloat(Object entity, long offset, float f)
    {
        unsafe.putFloat(entity, offset, f);
    }

    public static void putDouble(Object entity, long offset, double d)
    {
        unsafe.putDouble(entity, offset, d);
    }

    public static Object allocateInstance(Class<?> type)
    {
        try
        {
            return unsafe.allocateInstance(type);
        }
        catch (InstantiationException e)
        {
            ReflectUtil.throwException(e);
            return null;
        }
    }
}
