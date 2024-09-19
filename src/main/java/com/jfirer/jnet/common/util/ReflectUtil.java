package com.jfirer.jnet.common.util;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public final class ReflectUtil
{
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Map<String, ? extends Enum<?>> getAllEnumInstances(Class<? extends Enum<?>> type)
    {
        try
        {
            Method method = Class.class.getDeclaredMethod("enumConstantDirectory");
            method.setAccessible(true);
            return new HashMap((Map<String, ?>) method.invoke(type));
        }
        catch (Exception e)
        {
            throwException(e);
            return null;
        }
    }

    public static Class<?> wrapPrimitive(Class<?> type)
    {
        if (type.isPrimitive() == false)
        {
            throw new IllegalArgumentException();
        }
        if (type == int.class)
        {
            return Integer.class;
        }
        else if (type == short.class)
        {
            return Short.class;
        }
        else if (type == long.class)
        {
            return Long.class;
        }
        else if (type == float.class)
        {
            return Float.class;
        }
        else if (type == Double.class)
        {
            return Double.class;
        }
        else if (type == boolean.class)
        {
            return Boolean.class;
        }
        else if (type == byte.class)
        {
            return Byte.class;
        }
        else if (type == char.class)
        {
            return Character.class;
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }

    public static void throwException(Throwable t)
    {
        if (t == null)
        {
            throw new NullPointerException("传入的参数为null");
        }
        else
        {
            UNSAFE.unsafe.throwException(t);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void throwException0(Throwable t) throws E
    {
        throw (E) t;
    }
}
