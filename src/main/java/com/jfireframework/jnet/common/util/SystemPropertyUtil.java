package com.jfireframework.jnet.common.util;

public class SystemPropertyUtil
{
    public static int getInt(String propertyName, int defaultValue)
    {
        String value = System.getProperty(propertyName);
        return isNotBlank(value) ? Integer.valueOf(value) : defaultValue;
    }

    public static boolean getBoolean(String propertyName, boolean defaulValue)
    {
        String property = System.getProperty(propertyName);
        return isNotBlank(property) ? Boolean.valueOf(property) : defaulValue;
    }

    static boolean isNotBlank(String value)
    {
        if (value != null && "".equals(value))
        {
            return true;
        }
        else
        {
            return false;
        }
    }
}
