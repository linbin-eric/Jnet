package com.jfireframework.jnet.common.api;

public interface ChannelContextInitializer
{
    /**
     * 当通道实例被创建时触发，该方法实现体多用于进行处理器绑定
     *
     * @param channelContext
     */
    void onChannelContextInit(ChannelContext channelContext);
}
