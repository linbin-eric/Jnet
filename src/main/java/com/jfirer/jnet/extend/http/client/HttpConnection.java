package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessor;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.coder.HeartBeat;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.common.util.ReflectUtil;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiFunction;

@Data
@Slf4j
public class HttpConnection
{
    @Getter
    private final    int                 id;
    private final    ClientChannel       clientChannel;
    // LockSupport相关字段
    private volatile Thread              waitingThread;
    private volatile HttpReceiveResponse responseResult;
    private volatile WriteResult         result = WriteResult.NEED_RESULT;
    @Getter
    @Setter
    private volatile long                lastBorrowTime;

    enum WriteResult
    {
        NEED_RESULT, SUCCESS, CLOSE_OF_CONNECTION
    }

    public HttpConnection(String domain, int port, int secondsOfKeepAlive, int id, BiFunction<Pipeline, HttpConnection, HttpReceiveResponse> responseCreator)
    {
        this.id = id;
        ChannelConfig channelConfig = new ChannelConfig().setIp(domain).setPort(port);
        clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
            pipeline.addReadProcessor(new HttpReceiveResponseDecoder(HttpConnection.this, responseCreator));
            pipeline.addReadProcessor(new ReadProcessor<HttpReceiveResponse>()
            {
                @Override
                public void readFailed(Throwable e, ReadProcessorNode next)
                {
                    result = WriteResult.CLOSE_OF_CONNECTION;
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
                    result         = WriteResult.SUCCESS;
                    Thread waiting = waitingThread;
                    if (waiting != null)
                    {
                        waitingThread = null;
                        LockSupport.unpark(waiting);
                    }
                }
            });
            pipeline.addWriteProcessor(new HttpSendRequestEncoder());
            pipeline.addWriteProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
        });
        if (!clientChannel.connect())
        {
            ReflectUtil.throwException(new RuntimeException("无法连接" + domain + ":" + port, clientChannel.getConnectionException()));
        }
    }

    public boolean isConnectionClosed()
    {
        return !clientChannel.alive();
    }

    public HttpReceiveResponse write(HttpSendRequest request, int secondsOfTimeout) throws ClosedChannelException, SocketTimeoutException
    {
        if (isConnectionClosed())
        {
            log.error("连接关闭,地址:{}", clientChannel.pipeline().getRemoteAddressWithoutException());
            request.close();
            throw new ClosedChannelException();
        }
        // 设置等待状态
        waitingThread  = Thread.currentThread();
        responseResult = null;
        result         = WriteResult.NEED_RESULT;
        // 发送请求
        clientChannel.pipeline().fireWrite(request);
        long timeoutNanos = TimeUnit.SECONDS.toNanos(secondsOfTimeout);
        long startTime    = System.nanoTime();
        while (result == WriteResult.NEED_RESULT)
        {
            long elapsed   = System.nanoTime() - startTime;
            long remaining = timeoutNanos - elapsed;
            if (remaining <= 0)
            {
                // 超时处理
                waitingThread = null;
                String msg = clientChannel.alive() ? "通道仍然alive" : "通道已经失效";
                clientChannel.pipeline().shutdownInput();
                log.error("连接关闭，地址:{}", clientChannel.pipeline().getRemoteAddressWithoutException());
                throw new SocketTimeoutException(msg);
            }
            LockSupport.parkNanos(remaining);
            // 检查中断
            if (Thread.interrupted())
            {
                waitingThread = null;
                clientChannel.pipeline().shutdownInput();
                log.error("连接关闭，地址:{}", clientChannel.pipeline().getRemoteAddressWithoutException());
                throw new ClosedChannelException();
            }
        }
        // 重置状态并返回结果
        waitingThread = null;
        HttpReceiveResponse response = responseResult;
        if (result == WriteResult.CLOSE_OF_CONNECTION)
        {
            log.debug("收到链接终止响应,连接关闭,地址:{}", clientChannel.pipeline().getRemoteAddressWithoutException());
            clientChannel.pipeline().shutdownInput();
            throw new ClosedChannelException();
        }
        responseResult = null;
        return response;
    }

    public void close()
    {
        clientChannel.pipeline().shutdownInput();
    }
}
