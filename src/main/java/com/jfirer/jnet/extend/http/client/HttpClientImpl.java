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
        Connection connection = new Connection(request.getDoMain(), request.getPort());
        Recycler<ClientWrapper> recycler = map.computeIfAbsent(connection, c -> new Recycler<>(() -> {
            DefaultClient defaultClient = new DefaultClient();
            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.setIp(c.domain);
            channelConfig.setPort(c.port);
            BlockingQueue<Object> sync = new LinkedBlockingDeque<>();
            if (!defaultClient.connect(channelConfig, channelContext -> {
                Pipeline pipeline = channelContext.pipeline();
                pipeline.addReadProcessor(new HttpReceiveResponseDecoder());
                pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
                {
                    @Override
                    public void channelClose(ReadProcessorNode next)
                    {
                        System.out.println("通道关闭");
                        sync.offer(END_OF_CONNECTION);
                    }

                    @Override
                    public void read(HttpReceiveResponse response, ReadProcessorNode next)
                    {
                        sync.offer(response);
                    }
                });
                pipeline.addWriteProcessor(new HttpSendRequestEncoder());
            }))
            {
                ReflectUtil.throwException(new ConnectException("无法连接:" + c.domain + ":" + c.port));
            }
            return new ClientWrapper(defaultClient, sync);
        }, (clientWrapper, handler) -> clientWrapper.handler = handler));
        ClientWrapper clientWrapper = recycler.get();
        if (clientWrapper.client.alive() == false || (System.currentTimeMillis() - clientWrapper.lastRespoonseTime) > KEEP_ALIVE_TIME)
        {
            clientWrapper.client.close();
            System.out.println("过期");
            new RuntimeException().printStackTrace();
            do
            {
                clientWrapper = recycler.get();
                if (clientWrapper.client.alive() == false || (System.currentTimeMillis() - clientWrapper.lastRespoonseTime) > KEEP_ALIVE_TIME)
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
        clientWrapper.client.write(request);
        Object poll = clientWrapper.sync.poll(5, TimeUnit.DAYS);
        if (poll == END_OF_CONNECTION)
        {
            throw new ClosedChannelException();
        }
        else
        {
            ClientWrapper finalClientWrapper = clientWrapper;
            ((HttpReceiveResponse) poll).setOnClose(v -> {
                finalClientWrapper.lastRespoonseTime = System.currentTimeMillis();
                finalClientWrapper.handler.recycle(finalClientWrapper);
            });
            return (HttpReceiveResponse) poll;
        }
    }
}
