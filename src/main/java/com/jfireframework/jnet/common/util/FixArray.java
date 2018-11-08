package com.jfireframework.jnet.common.util;

public interface FixArray<E>
{

    boolean isEmpty();

    /**
     * 返回下一个可以写入的下标。如果返回-1意味着当前队列已经满了
     *
     * @return
     */
    long nextOfferIndex();

    boolean canOffer();

    /**
     * 返回下一个可以写入的下标，如果不存在，则使用Thread.yeild（）策略等待直到有可用为止
     *
     * @return
     */
    long waitUntilOfferIndexAvail();

    /**
     * 返回坐标上对应槽位的数据
     *
     * @param index
     * @return
     */
    E getSlot(long index);

    /**
     * 提交写入的数据
     *
     * @param index
     */
    void commit(long index);

    /**
     * 返回下一个可以消费的坐标.如果当前没有可以消费的数据，则返回-1
     *
     * @return
     */
    long nextAvail();

    /**
     * 消费完index坐标
     *
     * @param index
     */
    void comsumeAvail(long index);
}
