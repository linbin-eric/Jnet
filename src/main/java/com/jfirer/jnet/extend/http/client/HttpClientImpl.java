package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.client.ClientChannelImpl;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.recycler.Recycler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;

public class HttpClientImpl implements HttpClient
{
    record Connection(String domain, int port) {}

    private ConcurrentMap<Connection, Recycler<ClientConnection>> map = new ConcurrentHashMap<>();

    @Override
    public HttpReceiveResponse newCall(HttpSendRequest request) throws Exception
    {
        perfect(request);
        Connection                 connection       = new Connection(request.getDoMain(), request.getPort());
        Recycler<ClientConnection> recycler         = findRecycler(connection);
        ClientConnection           clientConnection = getAvailableClient(request, recycler);
        return writeAndWaitForResponse(request, clientConnection);
    }

    private static HttpReceiveResponse writeAndWaitForResponse(HttpSendRequest request, ClientConnection clientConnection) throws InterruptedException, ClosedChannelException
    {
        clientConnection.client.write(request);
        HttpReceiveResponse response = clientConnection.waitForResponse();
        response.setOnClose(v -> clientConnection.recycle());
        return response;
    }

    private Recycler<ClientConnection> findRecycler(Connection connection)
    {
        return map.computeIfAbsent(connection, c -> new Recycler<>(() -> {
            ChannelConfig channelConfig = new ChannelConfig();
            channelConfig.setIp(c.domain);
            channelConfig.setPort(c.port);
            BlockingQueue<HttpReceiveResponse> sync = new LinkedBlockingDeque<>();
            ClientChannel clientChannel = new ClientChannelImpl(channelConfig, channelContext -> {
                Pipeline pipeline = channelContext.pipeline();
                pipeline.addReadProcessor(new HttpReceiveResponseDecoder());
                pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
                {
                    @Override
                    public void channelClose(ReadProcessorNode next, Throwable e)
                    {
                        sync.offer(ClientConnection.CLOSE_OF_CONNECTION);
                        e.printStackTrace();
                    }

                    @Override
                    public void read(HttpReceiveResponse response, ReadProcessorNode next)
                    {
                        sync.offer(response);
                    }
                });
                pipeline.addWriteProcessor(new HttpSendRequestEncoder());
            });
            if (!clientChannel.connect())
            {
                ReflectUtil.throwException(new ConnectException("无法连接" + connection.domain + ":" + connection.port));
            }
            return new ClientConnection(clientChannel, sync);
        }, ClientConnection::setHandler));
    }

    private ClientConnection getAvailableClient(HttpSendRequest request, Recycler<ClientConnection> recycler)
    {
        ClientConnection clientConnection = null;
        try
        {
            clientConnection = recycler.get();
        }
        catch (Exception e)
        {
            ReflectUtil.throwException(e);
        }
        if (clientConnection.isConnectionClosed())
        {
            clientConnection.client.close();
            do
            {
                try
                {
                    clientConnection = recycler.get();
                }
                catch (Throwable e)
                {
                    ReflectUtil.throwException(e);
                }
                if (clientConnection.isConnectionClosed())
                {
                    clientConnection.client.close();
                }
                else
                {
                    return clientConnection;
                }
            }
            while (true);
        }
        else
        {
            return clientConnection;
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
