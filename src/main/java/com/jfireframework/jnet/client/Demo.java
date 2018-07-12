package com.jfireframework.jnet.client;

public class Demo
{
	public static void main(String[] args)
	{
		long l = 1l << 62;
		System.out.println(l);
		l = 1l << 63;
		System.out.println(l);
		System.out.println(Long.MIN_VALUE);
		l = 1l << 64;
		System.out.println(l);
		long avail = 1l << 61;
		System.out.println(avail);
		System.out.println(avail / 1000000 / 60 / 60 / 24 / 365);
	}
}
