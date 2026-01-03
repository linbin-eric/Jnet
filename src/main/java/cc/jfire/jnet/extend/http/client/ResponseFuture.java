package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpResponsePart;

public interface ResponseFuture
{
    /**
     * IO 线程调用，传入收到的 HTTP 响应片段
     */
    void onReceive(HttpResponsePart part);

    /**
     * IO 线程调用，传入通道读取异常
     */
    void onFail(Throwable error);
}

