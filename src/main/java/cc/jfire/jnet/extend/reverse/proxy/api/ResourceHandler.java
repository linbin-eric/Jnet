package cc.jfire.jnet.extend.reverse.proxy.api;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.dto.HttpRequestPart;

public interface ResourceHandler
{
    /**
     * 处理请求部分
     *
     * @param part     请求部分
     * @param pipeline 管道
     * @return true表示该handler处理了这个请求，false表示未处理
     */
    boolean process(HttpRequestPart part, Pipeline pipeline);

    default void readFailed(Throwable e) {}
}
