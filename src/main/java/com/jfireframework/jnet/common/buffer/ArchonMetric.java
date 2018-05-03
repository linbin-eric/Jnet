package com.jfireframework.jnet.common.buffer;

public class ArchonMetric
{
	private int	newChunkCount	= 0;
	private int	hitChunkStep1	= 0;
	private int	hitChunkStep2	= 0;
	private int	hitChunkStep3	= 0;
	private int	hitChunkStep4	= 0;
	private int	hitChunkStep5	= 0;
	private int	hitChunkStep6	= 0;
	
	public void newChunkRecord()
	{
		newChunkCount += 1;
	}
	
	public void hitChunkStep(int step)
	{
		switch (step)
		{
			case 1:
				hitChunkStep1 += 1;
				break;
			case 2:
				hitChunkStep2 += 1;
				break;
			case 3:
				hitChunkStep3 += 1;
				break;
			case 4:
				hitChunkStep4 += 1;
				break;
			case 5:
				hitChunkStep5 += 1;
				break;
			case 6:
				hitChunkStep6 += 1;
				break;
			default:
				throw new IllegalArgumentException();
		}
	}
	
	@Override
	public String toString()
	{
		return "ArchonMetric [newChunkCount=" + newChunkCount + ", hitChunkStep1=" + hitChunkStep1 + ", hitChunkStep2=" + hitChunkStep2 + ", hitChunkStep3=" + hitChunkStep3 + ", hitChunkStep4=" + hitChunkStep4 + ", hitChunkStep5=" + hitChunkStep5 + ", hitChunkStep6=" + hitChunkStep6 + "]";
	}
	
}
