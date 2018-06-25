package com.jfireframework.jnet.test.recycler;

import com.jfireframework.baseutil.encrypt.Md5Util;

public class Demo
{
	public static void main(String[] args)
	{
		System.out.println(Md5Util.md5Str("12"));
		System.out.println(new String(Md5Util.md5("12".getBytes())));
	}
}
