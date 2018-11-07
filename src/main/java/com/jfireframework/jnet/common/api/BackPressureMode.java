package com.jfireframework.jnet.common.api;

public class BackPressureMode
{
    /**
     * 是否开启背压模式
     */
    private boolean enable = false;
    /**
     * 当开启背压模式时，写完成器的队列长度
     */
    private int     queueCapacity;

    /**
     * 开启背压，设定了写完成器的队列长度。
     *
     * @param queueCapacity
     */
    public BackPressureMode(int queueCapacity)
    {
        enable = true;
        this.queueCapacity = queueCapacity;
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

    public String toString()
    {
        return String.valueOf(enable);
    }
}
