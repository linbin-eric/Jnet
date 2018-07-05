package com.jfireframework.jnet.common.util;

import com.jfireframework.baseutil.StringUtil;

public class SystemPropertyUtil
{
	public static int getInt(String propertyName, int defaultValue)
	{
		String value = System.getProperty(propertyName);
		return StringUtil.isNotBlank(value) ? Integer.valueOf(value) : defaultValue;
	}
	
	public static boolean getBoolean(String propertyName, boolean defaulValue)
	{
		String property = System.getProperty(propertyName);
		return StringUtil.isNotBlank(property) ? Boolean.valueOf(property) : defaulValue;
	}
}
