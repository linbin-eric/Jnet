package cc.jfire.jnet.extend.http.client;

import cc.jfire.baseutil.STR;
import cc.jfire.baseutil.TRACEID;
import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.HeartBeat;
import cc.jfire.jnet.common.internal.DefaultPipeline;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.common.util.ReflectUtil;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartEncoder;
import cc.jfire.jnet.extend.http.coder.HttpResponsePartDecoder;
import cc.jfire.jnet.extend.http.dto.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
public class HttpConnection
{
    @Getter
    private volatile ResponseFuture responseFuture;
    private final    ClientChannel  clientChannel;
    private final    AtomicBoolean  isClosed = new AtomicBoolean(false);
    private          String         uid      = TRACEID.newTraceId();

    public HttpConnection(String domain, int port, int secondsOfKeepAlive)
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
                    try
                    {
                        ResponseFuture future = responseFuture;
                        if (future == null)
                        {
                            part.free();
                            next.fireRead(null);
                            return;
                        }
                        // 先清除 responseFuture，再调用 onReceive
                        // 因为 onReceive 中可能会归还连接到连接池，必须确保归还前 responseFuture 已清空
                        if (part.isLast())
                        {
//                            log.debug("[HttpConnection:{},pipeline:{}] 收到最后一个响应体，清除当前 future", uid, ((DefaultPipeline) next.pipeline()).getUid());
                            responseFuture = null;
                        }
                        future.onReceive(part);
                    }
                    catch (Throwable e)
                    {
                        pipeline.shutdownInput();
                    }
                }

                @Override
                public void readFailed(Throwable e, ReadProcessorNode next)
                {
                    close();
                    ResponseFuture future = responseFuture;
                    responseFuture = null;
                    if (future != null)
                    {
                        future.onFail(e);
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
//        log.debug("[HttpConnection:{},pipeline:{}]创建", uid, ((DefaultPipeline) clientChannel.pipeline()).getUid());
    }

    public boolean isConnectionClosed()
    {
        return isClosed.get() || !clientChannel.alive();
    }

    /**
     * 检查连接是否有未完成的响应
     *
     * @return true 表示有未完成的响应，连接不应被复用
     */
    public boolean hasUnfinishedResponse()
    {
        return responseFuture != null;
    }

    /**
     * 带超时的 write 方法，返回完整的 HttpResponse
     */
    public HttpResponse write(HttpRequest request, int secondsOfTimeout) throws ClosedChannelException, SocketTimeoutException
    {
        if (isConnectionClosed())
        {
//            log.error("连接已关闭，地址：{}", clientChannel.pipeline().getRemoteAddressWithoutException());
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
    public StreamableResponseFuture write(HttpRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer) throws ClosedChannelException
    {
        if (isConnectionClosed())
        {
//            log.error("连接已关闭，地址：{}", clientChannel.pipeline().getRemoteAddressWithoutException());
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
            request.close();
            throw new ClosedChannelException();
        }
        if (this.responseFuture != null)
        {
            request.close();
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
//            log.error("HttpRequestPart 只能是 HttpRequestFixLengthBodyPart 或 HttpRequestChunkedBodyPart");
            body.close();
            ReflectUtil.throwException(new IllegalArgumentException("HttpRequestPart 只能是 HttpRequestFixLengthBodyPart 或 HttpRequestChunkedBodyPart"));
        }
    }

    public void close()
    {
        if (isClosed.compareAndExchange(false, true) == false)
        {
            clientChannel.pipeline().shutdownInput();
        }
    }

    @Override
    public String toString()
    {
        return STR.format("[http2connection:{},pipeline:{}]",uid, ((DefaultPipeline) clientChannel.pipeline()).getUid());
    }
}

