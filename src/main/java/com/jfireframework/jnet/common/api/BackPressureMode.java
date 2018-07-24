package com.jfireframework.jnet.common.api;

import com.jfireframework.jnet.common.util.MathUtil;

public class BackPressureMode
{
	/**
	 * 是否开启背压模式
	 */
	private boolean				enable	= false;
	/**
	 * 当开启背压模式时，写完成器的队列长度
	 */
	private int					queueCapacity;
	private BackPressureService	backPressureService;
	
	public BackPressureMode(int queueCapacity, BackPressureService backPressureService)
	{
		enable = true;
		if (enable)
		{
			this.queueCapacity = Math.max(512, MathUtil.normalizeSize(queueCapacity));
			this.backPressureService = backPressureService;
		}
	}
	
	public BackPressureMode()
	{
		enable = false;
	}
	
	/**
	 * 是否开启背压模式
	 * 
	 * @return
	 */
	public boolean isEnable()
	{
		return enable;
	}
	
	/**
	 * 背压模式开启时，写完成器的队列长度
	 * 
	 * @return
	 */
	public int getQueueCapacity()
	{
		return queueCapacity;
	}
	
	public BackPressureService getBackPressureService()
	{
		return backPressureService;
	}
	
}
