package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.util.ReflectUtil;

public interface HttpClient
{
    BufferAllocator    ALLOCATOR       = new PooledBufferAllocator(40000, true, PooledBufferAllocator.getArena(true));
    HttpConnectionPool CONNECTION_POOL = new HttpConnectionPool();

    static HttpReceiveResponse newCall(HttpSendRequest request) throws Exception
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
            HttpReceiveResponse response = httpConnection.write(request, 60);
            // 请求成功，归还连接
            CONNECTION_POOL.returnConnection(host, port, httpConnection);
            return response;
        }
        catch (Throwable e)
        {
            request.close();
            if (httpConnection != null)
            {
                // 异常时移除连接，不归还
                CONNECTION_POOL.removeConnection(host, port, httpConnection);
            }
            ReflectUtil.throwException(e);
            return null;
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
        int portStart = url.indexOf(':', domainStart);
        request.setPath(index == -1 ? "/" : url.substring(index));
        request.setPort(portStart == -1 ? 80 : Integer.parseInt(url.substring(portStart + 1, index)));
        request.setDoMain(portStart == -1 ? url.substring(domainStart, index) : url.substring(domainStart, portStart));
        request.putHeader("Host", url.substring(domainStart, index));
    }
}
