package com.jfireframework.jnet.common.buffer;

public abstract class Archon
{
    /**
     * 申请一段内存。如果申请失败则返回false <br/>
     * 
     * @param need
     * @param bucket
     * @return
     */
    public abstract void apply(int need, IoBuffer buffer);
    
    /**
     * 将Bucket中的内存回收。<br/>
     * 
     * @param bucket
     */
    public abstract void recycle(IoBuffer buffer);
    
    public abstract void recycle(IoBuffer[] buffers, int off, int len);
    
    /**
     * 对handler进行扩容，扩容流程是先申请一个newSize大小的空间，将handler本身的内容复制过去。然后将handler中的部分回收。<br/>
     * 
     * @param buffer
     * @param newSize
     */
    public abstract void expansion(IoBuffer buffer, int newSize);
}
