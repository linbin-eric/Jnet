package com.jfireframework.jnet.common.api;

import java.nio.channels.AsynchronousSocketChannel;
import com.jfireframework.jnet.common.buffer.IoBuffer;

public interface ChannelContext
{
    /**
     * 与当前通道关联的一个附属对象
     * 
     * @return
     */
    Object getAttachment();
    
    /**
     * 设置关联的附属对象
     * 
     * @param attachment
     */
    void setAttachment(Object attachment);
    
    /**
     * 处理从通道读取到的报文
     * 
     * @param packet
     * @throws Throwable
     */
    void read(IoBuffer packet) throws Throwable;
    
    /**
     * 向通道写出数据
     * 
     * @param buf
     * @throws Throwable
     */
    void write(IoBuffer buf);
    
    AsynchronousSocketChannel socketChannel();
    
}
