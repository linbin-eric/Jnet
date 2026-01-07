package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;

import java.util.function.Consumer;

public interface HttpClient
{
    HttpConnectionPool CONNECTION_POOL = new HttpConnectionPool();

    static HttpResponse newCall(HttpSendRequest request) throws Exception
    {
        perfect(request);
        String         host           = request.getDoMain();
        int            port           = request.getPort();
        HttpConnection httpConnection = null;
        try
        {
            // 从连接池借用连接（自动创建或复用）
            httpConnection = CONNECTION_POOL.borrowConnection(host, port, 60);
            // 执行请求
            HttpResponse response = httpConnection.write(request, 60);
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
        String          host           = request.getDomain();
        int            port           = request.getPort();
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

    private static void perfect(HttpSendRequest request)
    {
        String url         = request.getUrl();
        int    index       = 0;
        int    domainStart = 0;
        if (url.startsWith("http://"))
        {
            index       = url.indexOf("/", 8);
            domainStart = 7;
        }
        else if (url.startsWith("https://"))
        {
            index       = url.indexOf("/", 9);
            domainStart = 8;
        }
        if (index == -1)
        {
            index = url.length();
        }
        int portStart = url.indexOf(':', domainStart);
        request.setPath(index == url.length() ? "/" : url.substring(index));
        request.setPort(portStart == -1 ? 80 : Integer.parseInt(url.substring(portStart + 1, index)));
        request.setDoMain(portStart == -1 ? url.substring(domainStart, index) : url.substring(domainStart, portStart));
        request.putHeader("Host", url.substring(domainStart, index));
    }
}
