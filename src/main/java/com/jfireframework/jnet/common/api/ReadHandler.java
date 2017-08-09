package com.jfireframework.jnet.common.api;

import java.nio.channels.CompletionHandler;

public interface ReadHandler extends CompletionHandler<Integer, Void>
{
    public void registerRead();
}
