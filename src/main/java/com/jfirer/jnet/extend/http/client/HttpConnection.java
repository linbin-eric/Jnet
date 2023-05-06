package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.client.ClientChannelImpl;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.api.WorkerGroup;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import java.net.ConnectException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Data
public class HttpConnection
{
    @Setter(AccessLevel.NONE)
    private             BlockingQueue<HttpReceiveResponse> responseSync        = new LinkedBlockingQueue<>();
    @Setter(AccessLevel.NONE)
    private             ClientChannel                      clientChannel;
    private             long                               lastRespoonseTime;
    private             RecycleHandler<HttpConnection>     handler;
    public static final HttpReceiveResponse                CLOSE_OF_CONNECTION = new HttpReceiveResponse();
    public static final long                               KEEP_ALIVE_TIME     = 1000 * 60 * 5;
    public static final WorkerGroup                        HTTP_WORKER_GROUP   = new DefaultWorkerGroup();

    public HttpConnection(String domain, int port)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setIp(domain);
        channelConfig.setPort(port);
        channelConfig.setWorkerGroup(HTTP_WORKER_GROUP);
        clientChannel = new ClientChannelImpl(channelConfig, channelContext -> {
            Pipeline pipeline = channelContext.pipeline();
            pipeline.addReadProcessor(new HttpReceiveResponseDecoder());
            pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
            {
                @Override
                public void channelClose(ReadProcessorNode next, Throwable e)
                {
                    responseSync.offer(HttpConnection.CLOSE_OF_CONNECTION);
                }

                @Override
                public void read(HttpReceiveResponse response, ReadProcessorNode next)
                {
                    responseSync.offer(response);
                }
            });
            pipeline.addWriteProcessor(new HttpSendRequestEncoder());
        });
        if (!clientChannel.connect())
        {
            ReflectUtil.throwException(new ConnectException("无法连接" + domain + ":" + port));
        }
        lastRespoonseTime = System.currentTimeMillis();
    }

    public boolean isConnectionClosed()
    {
        return !clientChannel.alive() || responseSync.peek() == CLOSE_OF_CONNECTION || (System.currentTimeMillis() - lastRespoonseTime) > KEEP_ALIVE_TIME;
    }

    public HttpReceiveResponse write(HttpSendRequest request) throws ClosedChannelException
    {
        if (isConnectionClosed())
        {
            throw new ClosedChannelException();
        }
        clientChannel.write(request);
        HttpReceiveResponse response = null;
        try
        {
            response = responseSync.poll(20, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            clientChannel.close();
            throw new ClosedChannelException();
        }
        if (response == null || response == CLOSE_OF_CONNECTION)
        {
            clientChannel.close();
            throw new ClosedChannelException();
        }
        else
        {
            response.setOnClose(v -> recycle());
            return response;
        }
    }

    public void close()
    {
        clientChannel.close();
    }

    public void recycle()
    {
        lastRespoonseTime = System.currentTimeMillis();
        handler.recycle(this);
    }
}
