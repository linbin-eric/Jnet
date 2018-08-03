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
	
	/**
	 * 开启背压，设定写完成器的队列长度以及背压服务实例
	 * 
	 * @param queueCapacity
	 * @param backPressureService
	 */
	public BackPressureMode(int queueCapacity, BackPressureService backPressureService)
	{
		enable = true;
		this.queueCapacity = Math.max(512, MathUtil.normalizeSize(queueCapacity));
		this.backPressureService = backPressureService;
	}
	
	/**
	 * 开启背压，设定了写完成器的队列长度。
	 * 
	 * @param queueCapacity
	 */
	public BackPressureMode(int queueCapacity)
	{
		enable = true;
		this.queueCapacity = queueCapacity;
		backPressureService = null;
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
	
	public String toString()
	{
		return String.valueOf(enable);
	}
	
}
