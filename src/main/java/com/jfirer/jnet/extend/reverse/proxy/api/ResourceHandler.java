package com.jfirer.jnet.extend.reverse.proxy.api;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.extend.http.dto.HttpRequest;

public interface ResourceHandler
{
    boolean process(HttpRequest request, Pipeline pipeline);

    default void readFailed(Throwable e) {}
}
