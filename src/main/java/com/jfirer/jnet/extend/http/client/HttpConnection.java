package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Data
@Slf4j
public class HttpConnection
{
    public static final HttpReceiveResponse      CLOSE_OF_CONNECTION = new HttpReceiveResponse();
    public static final long                     KEEP_ALIVE_TIME     = 1000 * 60 * 5;
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

    private final ClientChannel                      clientChannel;
    private       long                               lastResponseTime;
    // LockSupport相关字段
    private volatile Thread                          waitingThread;
    private volatile HttpReceiveResponse             responseResult;
    private volatile boolean                         responseReady = false;

    public HttpConnection(String domain, int port)
    {
        ChannelConfig channelConfig = new ChannelConfig().setIp(domain).setPort(port).setChannelGroup(HTTP_CHANNEL_GROUP);
        clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new HttpReceiveResponseDecoder(this));
            pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
            {
                @Override
                public void readFailed(Throwable e, ReadProcessorNode next)
                {
                    responseResult = HttpConnection.CLOSE_OF_CONNECTION;
                    responseReady = true;
                    Thread waiting = waitingThread;
                    if (waiting != null)
                    {
                        LockSupport.unpark(waiting);
                    }
                }

                @Override
                public void read(HttpReceiveResponse response, ReadProcessorNode next)
                {
                    responseResult = response;
                    responseReady = true;
                    Thread waiting = waitingThread;
                    if (waiting != null)
                    {
                        LockSupport.unpark(waiting);
                    }
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
        return !clientChannel.alive() || (System.currentTimeMillis() - lastResponseTime) > KEEP_ALIVE_TIME;
    }

    public HttpReceiveResponse write(HttpSendRequest request) throws ClosedChannelException, SocketTimeoutException
    {
        if (isConnectionClosed())
        {
            request.freeBodyBuffer();
            throw new ClosedChannelException();
        }
        
        // 设置等待状态
        waitingThread = Thread.currentThread();
        responseReady = false;
        responseResult = null;
        
        // 发送请求
        clientChannel.pipeline().fireWrite(request);
        
        // 使用LockSupport等待响应，支持20秒超时
        long timeoutNanos = TimeUnit.SECONDS.toNanos(20);
        long startTime = System.nanoTime();
        
        while (!responseReady)
        {
            long elapsed = System.nanoTime() - startTime;
            long remaining = timeoutNanos - elapsed;
            
            if (remaining <= 0)
            {
                // 超时处理
                waitingThread = null;
                log.debug("超时等待20秒，没有收到响应，关闭Http链接");
                String msg = clientChannel.alive() ? "通道仍然alive" : "通道已经失效";
                clientChannel.pipeline().shutdownInput();
                throw new SocketTimeoutException(msg);
            }
            
            LockSupport.parkNanos(remaining);
            
            // 检查中断
            if (Thread.interrupted())
            {
                waitingThread = null;
                clientChannel.pipeline().shutdownInput();
                throw new ClosedChannelException();
            }
        }
        
        // 重置状态并返回结果
        waitingThread = null;
        HttpReceiveResponse response = responseResult;
        responseResult = null;
        responseReady = false;
        
        if (response == CLOSE_OF_CONNECTION)
        {
            log.debug("收到链接终止响应");
            clientChannel.pipeline().shutdownInput();
            throw new ClosedChannelException();
        }
        
        return response;
    }

    public void close()
    {
        clientChannel.pipeline().shutdownInput();
    }

}
