package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.DefaultClient;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.*;

public class HttpClientImpl implements HttpClient
{
    record Connection(String domain, int port) {}

    private              ConcurrentMap<Connection, Recycler<ClientWrapper>> map               = new ConcurrentHashMap<>();
    private static final Object                                             END_OF_CONNECTION = new Object();
    private static final long                                               KEEP_ALIVE_TIME   = 1000 * 60 * 5;

    @Override
    public HttpReceiveResponse newCall(HttpSendRequest request) throws Exception
    {
        perfect(request);
        Connection              connection    = new Connection(request.getDoMain(), request.getPort());
        Recycler<ClientWrapper> recycler      = findRecycler(connection);
        ClientWrapper           clientWrapper = getAvailableClient(request, recycler);
        return writeAndWaitForResponse(request, clientWrapper);
    }

    private static HttpReceiveResponse writeAndWaitForResponse(HttpSendRequest request, ClientWrapper clientWrapper) throws InterruptedException, ClosedChannelException
    {
        clientWrapper.client.write(request);
        Object poll = clientWrapper.sync.poll(5, TimeUnit.DAYS);
        if (poll == END_OF_CONNECTION)
        {
            throw new ClosedChannelException();
        }
        else
        {
            ((HttpReceiveResponse) poll).setOnClose(v -> {
                clientWrapper.lastRespoonseTime = System.currentTimeMillis();
                clientWrapper.handler.recycle(clientWrapper);
            });
            return (HttpReceiveResponse) poll;
        }
    }

    private Recycler<ClientWrapper> findRecycler(Connection connection)
    {
        Recycler<ClientWrapper> recycler = map.computeIfAbsent(connection, c -> new Recycler<>(() -> {
            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.setIp(c.domain);
            channelConfig.setPort(c.port);
            BlockingQueue<Object> sync = new LinkedBlockingDeque<>();
            DefaultClient defaultClient = new DefaultClient(channelConfig, channelContext -> {
                Pipeline pipeline = channelContext.pipeline();
                pipeline.addReadProcessor(new HttpReceiveResponseDecoder());
                pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
                {
                    @Override
                    public void channelClose(ReadProcessorNode next)
                    {
                        sync.offer(END_OF_CONNECTION);
                    }

                    @Override
                    public void read(HttpReceiveResponse response, ReadProcessorNode next)
                    {
                        sync.offer(response);
                    }
                });
                pipeline.addWriteProcessor(new HttpSendRequestEncoder());
            });
            return new ClientWrapper(defaultClient, sync);
        }, (clientWrapper, handler) -> clientWrapper.handler = handler));
        return recycler;
    }

    private ClientWrapper getAvailableClient(HttpSendRequest request, Recycler<ClientWrapper> recycler)
    {
        ClientWrapper clientWrapper = recycler.get();
        if (!clientWrapper.client.connect())
        {
            if (request.getBody() != null)
            {
                request.getBody().free();
            }
            clientWrapper.client.close();
            ReflectUtil.throwException(new ConnectException("无法连接:" + request.getDoMain() + ":" + request.getPort()));
        }
        if (!clientWrapper.client.alive() || (System.currentTimeMillis() - clientWrapper.lastRespoonseTime) > KEEP_ALIVE_TIME)
        {
            clientWrapper.client.close();
            do
            {
                clientWrapper = recycler.get();
                if (!clientWrapper.client.alive() || (System.currentTimeMillis() - clientWrapper.lastRespoonseTime) > KEEP_ALIVE_TIME)
                {
                    clientWrapper.client.close();
                }
                else
                {
                    break;
                }
            }
            while (true);
        }
        return clientWrapper;
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
