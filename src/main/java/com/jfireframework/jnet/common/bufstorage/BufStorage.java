package com.jfireframework.jnet.common.bufstorage;

import com.jfireframework.baseutil.collection.buffer.ByteBuf;

public interface BufStorage
{
    public static enum StorageType
    {
        spsc, mpsc
    }
    
    public StorageType type();
    
    /**
     * 放入一个需要发送的buf.返回放入的结果
     * 
     * @param buf
     */
    public boolean putBuf(ByteBuf<?> buf);
    
    /**
     * 取得下一个要发送的buf数据。<br>
     * 注意：调用该方法如果返回不为null，则返回值就已经从存储中被取出
     * 
     * @return
     */
    public ByteBuf<?> next();
    
    public boolean isEmpty();
    
    /**
     * 获取最多max个数据,将数据放入store中。返回真实放入的数据个数。该结果可能小于max
     * 
     * @return
     */
    public int batchNext(ByteBuf<?>[] store, int max);
    
}
