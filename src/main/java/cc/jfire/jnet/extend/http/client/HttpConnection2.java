package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.HeartBeat;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.common.util.ReflectUtil;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartEncoder;
import cc.jfire.jnet.extend.http.coder.HttpResponsePartDecoder;
import cc.jfire.jnet.extend.http.dto.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.function.Consumer;

@Slf4j
public class HttpConnection2
{
    @Getter
    private final    ClientChannel  clientChannel;
    private volatile ResponseFuture responseFuture;

    public HttpConnection2(String domain, int port, int secondsOfKeepAlive)
    {
        ChannelConfig channelConfig = new ChannelConfig().setIp(domain).setPort(port);
        clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
            pipeline.addReadProcessor(new HttpResponsePartDecoder());
            pipeline.addReadProcessor(new ReadProcessor<HttpResponsePart>()
            {
                @Override
                public void read(HttpResponsePart part, ReadProcessorNode next)
                {
                    ResponseFuture future = responseFuture;
                    if (future == null)
                    {
                        part.free();
                        next.fireRead(null);
                        return;
                    }
                    future.onReceive(part);
                    if (part.isLast())
                    {
                        responseFuture = null;
                    }
                    next.fireRead(null);
                }

                @Override
                public void readFailed(Throwable e, ReadProcessorNode next)
                {
                    ResponseFuture future = responseFuture;
                    if (future != null)
                    {
                        future.onFail(e);
                        responseFuture = null;
                    }
                }
            });
            pipeline.addWriteProcessor(new HttpRequestPartEncoder());
            pipeline.addWriteProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
        });
        if (!clientChannel.connect())
        {
            ReflectUtil.throwException(new RuntimeException("无法连接 " + domain + ":" + port, clientChannel.getConnectionException()));
        }
    }

    public boolean isConnectionClosed()
    {
        return !clientChannel.alive();
    }

    /**
     * 带超时的 write 方法，返回完整的 HttpResponse
     */
    public HttpResponse write(HttpSendRequest request, int secondsOfTimeout) throws ClosedChannelException, SocketTimeoutException
    {
        if (isConnectionClosed())
        {
            log.error("连接已关闭，地址：{}", clientChannel.pipeline().getRemoteAddressWithoutException());
            request.close();
            throw new ClosedChannelException();
        }
        AggregatorResponseFuture aggregator = new AggregatorResponseFuture(clientChannel.pipeline().allocator());
        this.responseFuture = aggregator;
        clientChannel.pipeline().fireWrite(request);
        try
        {
            return aggregator.waitForEnd(secondsOfTimeout);
        }
        catch (SocketTimeoutException e)
        {
            clientChannel.pipeline().shutdownInput();
            throw e;
        }
        catch (Exception e)
        {
            clientChannel.pipeline().shutdownInput();
            if (e instanceof ClosedChannelException ex)
            {
                throw ex;
            }
            ReflectUtil.throwException(e);
            return null;
        }
    }

    /**
     * 流式 write 方法，返回 StreamableResponseFuture
     */
    public StreamableResponseFuture write(HttpSendRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws ClosedChannelException
    {
        if (isConnectionClosed())
        {
            log.error("连接已关闭，地址：{}", clientChannel.pipeline().getRemoteAddressWithoutException());
            request.close();
            throw new ClosedChannelException();
        }
        StreamableResponseFuture streamable = new StreamableResponseFuture(partConsumer, errorConsumer);
        this.responseFuture = streamable;
        clientChannel.pipeline().fireWrite(request);
        return streamable;
    }

    public StreamableResponseFuture write(HttpRequestPartHead request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws ClosedChannelException
    {
        if (isConnectionClosed())
        {
            log.error("连接已关闭，地址：{}", clientChannel.pipeline().getRemoteAddressWithoutException());
            request.close();
            throw new ClosedChannelException();
        }
        if (this.responseFuture != null)
        {
            request.close();
            log.error("上一个响应还没有收到完全，不应该发起新的 Http 请求");
            ReflectUtil.throwException(new IllegalStateException("上一个响应还没有收到完全，不应该发起新的 Http 响应"));
        }
        StreamableResponseFuture streamable = new StreamableResponseFuture(partConsumer, errorConsumer);
        this.responseFuture = streamable;
        clientChannel.pipeline().fireWrite(request);
        return streamable;
    }

    public void write(HttpRequestPart body)
    {
        if (body instanceof HttpRequestFixLengthBodyPart || body instanceof HttpRequestChunkedBodyPart)
        {
            clientChannel.pipeline().fireWrite(body);
        }
        else
        {
            log.error("HttpRequestPart 只能是 HttpRequestFixLengthBodyPart 或 HttpRequestChunkedBodyPart");
            body.close();
            ReflectUtil.throwException(new IllegalArgumentException("HttpRequestPart 只能是 HttpRequestFixLengthBodyPart 或 HttpRequestChunkedBodyPart"));
        }
    }

    public void close()
    {
        clientChannel.pipeline().shutdownInput();
    }
}

