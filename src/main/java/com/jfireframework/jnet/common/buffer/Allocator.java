package com.jfireframework.jnet.common.buffer;

import com.jfireframework.jnet.common.util.SystemPropertyUtil;

public class Allocator
{
	public static final int	PAGESIZE;
	public static final int	MAXLEVEL;
	public static final int	CHUNKSIZE;
	static
	{
		int maxLevel = SystemPropertyUtil.getInt("io.jnet.arena.maxLevel", 11);
		MAXLEVEL = Math.min(maxLevel, 30);
		int pageSize = SystemPropertyUtil.getInt("io.jnet.arena.pageSize", 8 * 1024);
		PAGESIZE = Math.max(pageSize, 4 * 1024);
		CHUNKSIZE = (1 << MAXLEVEL) * PAGESIZE;
	}
}
