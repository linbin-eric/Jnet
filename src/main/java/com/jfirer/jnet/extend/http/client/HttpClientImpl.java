package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HttpClientImpl implements HttpClient
{
    record Connection(String domain, int port) {}

    private final ConcurrentMap<Connection, Recycler<HttpConnection>> map = new ConcurrentHashMap<>();

    @Override
    public HttpReceiveResponse newCall(HttpSendRequest request) throws Exception
    {
        perfect(request);
        Connection               connection     = new Connection(request.getDoMain(), request.getPort());
        Recycler<HttpConnection> recycler       = map.computeIfAbsent(connection, c -> new Recycler<>(() -> new HttpConnection(c.domain, c.port), HttpConnection::setHandler));
        HttpConnection           httpConnection = getAvailableConnection(recycler);
        return httpConnection.write(request);
    }

    private HttpConnection getAvailableConnection(Recycler<HttpConnection> recycler)
    {
        HttpConnection httpConnection = null;
        try
        {
            httpConnection = recycler.get();
        }
        catch (Exception e)
        {
            ReflectUtil.throwException(e);
        }
        if (httpConnection.isConnectionClosed())
        {
            httpConnection.close();
            do
            {
                try
                {
                    httpConnection = recycler.get();
                }
                catch (Throwable e)
                {
                    ReflectUtil.throwException(e);
                }
                if (httpConnection.isConnectionClosed())
                {
                    httpConnection.close();
                }
                else
                {
                    return httpConnection;
                }
            }
            while (true);
        }
        else
        {
            return httpConnection;
        }
    }

    private static void perfect(HttpSendRequest request)
    {
        String url         = request.getUrl();
        int    index       = 0;
        int    domainStart = 0;
        if (url.startsWith("http://"))
        {
            index = url.indexOf("/", 8);
            domainStart = 7;
        }
        else if (url.startsWith("https://"))
        {
            index = url.indexOf("/", 9);
            domainStart = 8;
        }
        int portStart = url.indexOf(':', domainStart);
        request.setPath(index == -1 ? "/" : url.substring(index));
        request.setPort(portStart == -1 ? 80 : Integer.parseInt(url.substring(portStart + 1, index)));
        request.setDoMain(portStart == -1 ? url.substring(domainStart, index) : url.substring(domainStart, portStart));
        request.putHeader("Host", url.substring(domainStart, index));
    }
}
