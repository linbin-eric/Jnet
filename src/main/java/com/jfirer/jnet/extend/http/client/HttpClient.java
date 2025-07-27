package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.allocator.BufferAllocator;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.util.ReflectUtil;

public interface HttpClient
{
    BufferAllocator ALLOCATOR = new PooledBufferAllocator(40000, true, PooledBufferAllocator.getArena(true));

    static HttpReceiveResponse newCall(HttpSendRequest request) throws Exception
    {
        perfect(request);
        HttpConnection httpConnection = null;
        try
        {
            // 每次请求都创建新的连接（已移除连接池功能）
            httpConnection = new HttpConnection(request.getDoMain(), request.getPort());
        }
        catch (Throwable e)
        {
            request.close();
            ReflectUtil.throwException(e);
        }
        return httpConnection.write(request);
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
