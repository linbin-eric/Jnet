package com.jfirer.jnet.common.api;

import java.nio.channels.CompletionHandler;

public interface ReadCompletionHandler extends CompletionHandler<Integer, ReadCompletionHandler>
{
    /**
     * 开始监听数据
     */
    void start();
}
