package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;

import java.util.function.Consumer;

public class HttpClient
{
    private static final HttpClient DEFAULT_INSTANCE = new HttpClient();

    private final HttpClientConfig   config;
    private final HttpConnectionPool connectionPool;

    public HttpClient()
    {
        this(new HttpClientConfig());
    }

    public HttpClient(HttpClientConfig config)
    {
        this.config         = config;
        this.connectionPool = new HttpConnectionPool(config);
    }

    public HttpClientConfig getConfig()
    {
        return config;
    }

    public HttpConnectionPool getConnectionPool()
    {
        return connectionPool;
    }

    /**
     * 发起同步 HTTP 请求
     */
    public cc.jfire.jnet.extend.http.dto.HttpResponse call(HttpRequest request) throws Exception
    {
        String         host           = request.getHead().getDomain();
        int            port           = request.getHead().getPort();
        boolean        ssl            = request.isSsl();
        HttpConnection httpConnection = null;
        try
        {
            httpConnection = connectionPool.borrowConnection(host, port, ssl);
            cc.jfire.jnet.extend.http.dto.HttpResponse response = httpConnection.write(request, config.getReadTimeoutSeconds());
            connectionPool.returnConnection(host, port, ssl, httpConnection);
            return response;
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

    /**
     * 发起流式 HTTP 请求
     */
    public StreamableResponseFuture streamCall(HttpRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws Exception
    {
        String         host           = request.getHead().getDomain();
        int            port           = request.getHead().getPort();
        boolean        ssl            = request.isSsl();
        HttpConnection httpConnection = null;
        try
        {
            httpConnection = connectionPool.borrowConnection(host, port, ssl);
            final HttpConnection finalConnection = httpConnection;
            final boolean        finalSsl        = ssl;
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
                        connectionPool.returnConnection(host, port, finalSsl, finalConnection);
                    }
                }
            };
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

    // ==================== 静态便捷方法（向后兼容） ====================

    /**
     * 使用默认实例发起同步 HTTP 请求（向后兼容）
     */
    public static cc.jfire.jnet.extend.http.dto.HttpResponse newCall(HttpRequest request) throws Exception
    {
        return DEFAULT_INSTANCE.call(request);
    }

    /**
     * 使用默认实例发起流式 HTTP 请求（向后兼容）
     */
    public static StreamableResponseFuture newStreamCall(HttpRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws Exception
    {
        return DEFAULT_INSTANCE.streamCall(request, partConsumer, errorConsumer);
    }

    /**
     * 获取默认实例
     */
    public static HttpClient getDefault()
    {
        return DEFAULT_INSTANCE;
    }
}
