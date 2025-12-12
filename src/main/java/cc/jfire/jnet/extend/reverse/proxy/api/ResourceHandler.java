package cc.jfire.jnet.extend.reverse.proxy.api;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.HttpRequest;

public interface ResourceHandler
{
    boolean process(HttpRequest request, Pipeline pipeline);

    default void readFailed(Throwable e) {}
}
