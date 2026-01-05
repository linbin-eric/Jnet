package cc.jfire.jnet.extend.reverse.proxy.api;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.HttpRequestPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

public interface ResourceHandler
{
    /**
     * 判断是否能处理该请求头
     */
    boolean match(HttpRequestPartHead head);

    /**
     * 处理请求部分
     */
    void process(HttpRequestPart part, Pipeline pipeline);

    default void readFailed(Throwable e) {}
}
