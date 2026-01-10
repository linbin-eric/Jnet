package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;

import java.util.function.Consumer;

public interface HttpClient
{
    HttpConnectionPool CONNECTION_POOL = new HttpConnectionPool();

    static cc.jfire.jnet.extend.http.dto.HttpResponse newCall(HttpRequest request) throws Exception
    {
        String         host           = request.getHead().getDomain();
        int            port           = request.getHead().getPort();
        HttpConnection httpConnection = null;
        try
        {
            // 从连接池借用连接（自动创建或复用）
            httpConnection = CONNECTION_POOL.borrowConnection(host, port, 60);
            // 执行请求
            cc.jfire.jnet.extend.http.dto.HttpResponse response = httpConnection.write(request, 60);
            CONNECTION_POOL.returnConnection(host, port, httpConnection);
            return response;
        }
        catch (Throwable e)
        {
            request.close();
            if (httpConnection != null)
            {
                // 异常时关闭连接，连接池会自动处理
                httpConnection.close();
            }
            throw e;
        }
    }

    static StreamableResponseFuture newStreamCall(HttpRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws Exception
    {
        String         host           = request.getHead().getDomain();
        int            port           = request.getHead().getPort();
        HttpConnection httpConnection = null;
        try
        {
            httpConnection = CONNECTION_POOL.borrowConnection(host, port, 60);
            final HttpConnection finalConnection = httpConnection;
            // 包装 partConsumer，在响应完成时归还连接
            Consumer<HttpResponsePart> wrappedPartConsumer = part -> {
                try
                {
                    if (partConsumer != null)
                    {
                        partConsumer.accept(part);
                    }
                }
                finally
                {
                    if (part.isLast())
                    {
                        CONNECTION_POOL.returnConnection(host, port, finalConnection);
                    }
                }
            };
            // 包装 errorConsumer，在发生错误时关闭连接
            Consumer<Throwable> wrappedErrorConsumer = error -> {
                finalConnection.close();
                if (errorConsumer != null)
                {
                    errorConsumer.accept(error);
                }
            };
            return httpConnection.write(request, wrappedPartConsumer, wrappedErrorConsumer);
        }
        catch (Throwable e)
        {
            request.close();
            if (httpConnection != null)
            {
                httpConnection.close();
            }
            throw e;
        }
    }
}
