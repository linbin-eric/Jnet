package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.api.WorkerGroup;
import com.jfirer.jnet.common.internal.DefaultWorkerGroup;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import com.jfirer.jnet.common.thread.FastThreadLocalThread;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@Data
@Slf4j
public class HttpConnection
{
    public static final HttpReceiveResponse      CLOSE_OF_CONNECTION = new HttpReceiveResponse(null);
    public static final long                     KEEP_ALIVE_TIME     = 1000 * 60 * 5;
    public static final WorkerGroup              HTTP_WORKER_GROUP   = new DefaultWorkerGroup(Runtime.getRuntime().availableProcessors(), "http_connection_worker_");
    public static final AsynchronousChannelGroup HTTP_CHANNEL_GROUP;

    static
    {
        try
        {
            HTTP_CHANNEL_GROUP = AsynchronousChannelGroup.withFixedThreadPool(Runtime.getRuntime().availableProcessors(), r -> new FastThreadLocalThread(r, "http-connection-channel-"));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private final BlockingQueue<HttpReceiveResponse> responseSync = new LinkedBlockingQueue<>();
    private final ClientChannel                      clientChannel;
    private       long                               lastResponseTime;
    private       RecycleHandler                     handler;

    public HttpConnection(String domain, int port)
    {
        ChannelConfig channelConfig = new ChannelConfig().setIp(domain).setPort(port).setChannelGroup(HTTP_CHANNEL_GROUP).setWorkerGroup(HTTP_WORKER_GROUP);
        clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new HttpReceiveResponseDecoder(this));
            pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
            {
                @Override
                public void readFailed(Throwable e, ReadProcessorNode next)
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
        lastResponseTime = System.currentTimeMillis();
    }

    public boolean isConnectionClosed()
    {
        return !clientChannel.alive() || responseSync.peek() == CLOSE_OF_CONNECTION || (System.currentTimeMillis() - lastResponseTime) > KEEP_ALIVE_TIME;
    }

    public HttpReceiveResponse write(HttpSendRequest request) throws ClosedChannelException, SocketTimeoutException
    {
        if (isConnectionClosed())
        {
            request.freeBodyBuffer();
            throw new ClosedChannelException();
        }
        clientChannel.pipeline().fireWrite(request);
        HttpReceiveResponse response = null;
        try
        {
            response = responseSync.poll(20, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            clientChannel.pipeline().shutdownInput();
            throw new ClosedChannelException();
        }
        if (response == null)
        {
            log.debug("超时等待20秒，没有收到响应，关闭Http链接");
            String msg = clientChannel.alive() ? "通道仍然alive" : "通道已经失效";
            clientChannel.pipeline().shutdownInput();
            SocketTimeoutException socketTimeoutException = new SocketTimeoutException(msg);
            throw socketTimeoutException;
        }
        if (response == CLOSE_OF_CONNECTION)
        {
            log.debug("收到链接终止响应");
            clientChannel.pipeline().shutdownInput();
            ClosedChannelException closedChannelException = new ClosedChannelException();
            throw closedChannelException;
        }
        else
        {
            return response;
        }
    }

    public void close()
    {
        clientChannel.pipeline().shutdownInput();
    }

    /**
     * 在一个HttpConnect 完成请求发送，响应解析并且业务端代码使用完毕后，就可以将这个链接归还到连接池中。
     */
    public void recycle()
    {
        lastResponseTime = System.currentTimeMillis();
        handler.recycle(this);
    }
}
