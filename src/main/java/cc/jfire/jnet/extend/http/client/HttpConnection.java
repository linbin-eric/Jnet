package cc.jfire.jnet.extend.http.client;

import cc.jfire.baseutil.TRACEID;
import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.HeartBeat;
import cc.jfire.jnet.common.internal.DefaultPipeline;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.common.util.ReflectUtil;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.*;
import lombok.Getter;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class HttpConnection
{
    private final    ClientChannel  clientChannel;
    private final    AtomicBoolean  isClosed = new AtomicBoolean(false);
    @Getter
    private volatile ResponseFuture responseFuture;
    private final    String         uid      = TRACEID.newTraceId();

    public HttpConnection(String domain, int port, int secondsOfKeepAlive)
    {
        this(domain, port, new HttpClientConfig().setKeepAliveSeconds(secondsOfKeepAlive), null, 0, false);
    }

    public HttpConnection(String domain, int port, int secondsOfKeepAlive, boolean ssl)
    {
        this(domain, port, new HttpClientConfig().setKeepAliveSeconds(secondsOfKeepAlive), null, 0, ssl);
    }

    public HttpConnection(String domain, int port, HttpClientConfig config, boolean ssl)
    {
        this(domain, port, config, null, 0, ssl);
    }

    /**
     * 通过 HTTP 代理创建连接（支持 HTTP/HTTPS 两种模式）
     *
     * @param domain    目标服务器域名
     * @param port      目标服务器端口
     * @param config    客户端配置
     * @param proxyHost 代理服务器主机名
     * @param proxyPort 代理服务器端口
     * @param ssl       是否使用 SSL（true 为 HTTPS 代理隧道模式，false 为 HTTP 直接代理模式）
     */
    public HttpConnection(String domain, int port, HttpClientConfig config, String proxyHost, int proxyPort, boolean ssl)
    {
        if (proxyHost == null)
        {
            // 直接连接模式(不使用代理)
            ChannelConfig channelConfig    = new ChannelConfig().setIp(domain).setPort(port);
            int           keepAliveSeconds = config.getKeepAliveSeconds();
            clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
                if (ssl)
                {
                    try
                    {
                        TrustManager[] trustManagers = config.getTrustManagers();
                        SSLContext     sslContext    = SSLContext.getInstance("TLS");
                        sslContext.init(null, trustManagers, null);
                        SSLEngine sslEngine = sslContext.createSSLEngine(domain, port);
                        sslEngine.setUseClientMode(true);
                        ClientSSLDecoder sslDecoder = new ClientSSLDecoder(sslEngine);
                        SSLEncoder       sslEncoder = new SSLEncoder(sslEngine);
                        sslEngine.beginHandshake();
                        pipeline.addReadProcessor(sslDecoder);
                        pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
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
                                    if (part.isLast())
                                    {
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
                        pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                        pipeline.addWriteProcessor(sslEncoder);
                        ((DefaultPipeline) pipeline).putPersistenceStore(ClientSSLDecoder.KEY, sslDecoder);
                    }
                    catch (Exception e)
                    {
                        ReflectUtil.throwException(new RuntimeException("SSL 初始化失败", e));
                    }
                }
                else
                {
                    pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
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
                                if (part.isLast())
                                {
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
                    pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                }
            });
            if (!clientChannel.connect())
            {
                ReflectUtil.throwException(new RuntimeException("无法连接 " + domain + ":" + port, clientChannel.getConnectionException()));
            }
            if (ssl)
            {
                ClientSSLDecoder sslDecoder = (ClientSSLDecoder) ((DefaultPipeline) clientChannel.pipeline()).getPersistenceStore(ClientSSLDecoder.KEY);
                sslDecoder.startHandshake(clientChannel.pipeline());
                try
                {
                    if (!sslDecoder.waitHandshake(config.getSslHandshakeTimeoutSeconds(), TimeUnit.SECONDS))
                    {
                        clientChannel.pipeline().shutdownInput();
                        ReflectUtil.throwException(new RuntimeException("SSL 握手超时"));
                    }
                }
                catch (InterruptedException e)
                {
                    clientChannel.pipeline().shutdownInput();
                    ReflectUtil.throwException(new RuntimeException("SSL 握手被中断", e));
                }
            }
        }
        else if (ssl)
        {
            // HTTPS 代理（CONNECT 隧道模式）
            ChannelConfig channelConfig      = new ChannelConfig().setIp(proxyHost).setPort(proxyPort);
            int           secondsOfKeepAlive = config.getKeepAliveSeconds();
            // 创建隧道读处理器
            ProxyTunnelReadHandler tunnelReadHandler = new ProxyTunnelReadHandler(domain, port);
            clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
                try
                {
                    // 初始化 SSL 引擎
                    TrustManager[] trustAllCerts = config.getTrustManagers();
                    SSLContext     sslContext    = SSLContext.getInstance("TLS");
                    sslContext.init(null, trustAllCerts, null);
                    SSLEngine sslEngine = sslContext.createSSLEngine(domain, port);
                    sslEngine.setUseClientMode(true);
                    ClientSSLDecoder sslDecoder = new ClientSSLDecoder(sslEngine);
                    SSLEncoder       sslEncoder = new SSLEncoder(sslEngine);
                    sslEngine.beginHandshake();
                    // 配置读取链: ProxyTunnelReadHandler -> ClientSSLDecoder -> HeartBeat -> HttpResponsePartDecoder -> 业务处理器
                    pipeline.addReadProcessor(tunnelReadHandler);
                    pipeline.addReadProcessor(sslDecoder);
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
                                if (part.isLast())
                                {
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
                    // 配置写入链: HttpRequestPartEncoder -> HeartBeat -> ClientSSLEncoder
                    pipeline.addWriteProcessor(new HttpRequestPartEncoder());
                    pipeline.addWriteProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
                    pipeline.addWriteProcessor(sslEncoder);
                    ((DefaultPipeline) pipeline).putPersistenceStore(ClientSSLDecoder.KEY, sslDecoder);
                    ((DefaultPipeline) pipeline).putPersistenceStore(ProxyTunnelReadHandler.KEY, tunnelReadHandler);
                }
                catch (Exception e)
                {
                    tunnelReadHandler.setTunnelError(e);
                }
            });
            if (!clientChannel.connect())
            {
                ReflectUtil.throwException(new RuntimeException("无法连接到代理服务器 " + proxyHost + ":" + proxyPort, clientChannel.getConnectionException()));
            }
            // 等待隧道建立
            try
            {
                if (!tunnelReadHandler.awaitTunnelEstablished(30, TimeUnit.SECONDS))
                {
                    clientChannel.pipeline().shutdownInput();
                    ReflectUtil.throwException(new RuntimeException("代理隧道建立超时"));
                }
            }
            catch (InterruptedException e)
            {
                clientChannel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("代理连接被中断", e));
            }
            if (tunnelReadHandler.getTunnelError() != null)
            {
                clientChannel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("代理隧道建立失败", tunnelReadHandler.getTunnelError()));
            }
            if (!tunnelReadHandler.isTunnelEstablished())
            {
                clientChannel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("代理服务器拒绝 CONNECT 请求"));
            }
            ClientSSLDecoder sslDecoder = (ClientSSLDecoder) ((DefaultPipeline) clientChannel.pipeline()).getPersistenceStore(ClientSSLDecoder.KEY);
            // 等待 SSL 握手完成
            if (sslDecoder != null)
            {
                try
                {
                    if (!sslDecoder.waitHandshake(30, TimeUnit.SECONDS))
                    {
                        clientChannel.pipeline().shutdownInput();
                        ReflectUtil.throwException(new RuntimeException("SSL 握手超时"));
                    }
                }
                catch (InterruptedException e)
                {
                    clientChannel.pipeline().shutdownInput();
                    ReflectUtil.throwException(new RuntimeException("SSL 握手被中断", e));
                }
            }
        }
        else
        {
            // HTTP 代理（直接代理模式）
            int           secondsOfKeepAlive = config.getKeepAliveSeconds();
            ChannelConfig channelConfig      = new ChannelConfig().setIp(proxyHost).setPort(proxyPort);
            clientChannel = ClientChannel.newClient(channelConfig, pipeline -> {
                // 配置读取链: HeartBeat -> HttpResponsePartDecoder -> 业务处理器
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
                            if (part.isLast())
                            {
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
                // 配置写入链: ProxyHttpRequestEncoder -> HeartBeat
                pipeline.addWriteProcessor(new ProxyHttpRequestEncoder(domain, port));
                pipeline.addWriteProcessor(new HeartBeat(secondsOfKeepAlive, pipeline));
            });
            if (!clientChannel.connect())
            {
                ReflectUtil.throwException(new RuntimeException("无法连接到代理服务器 " + proxyHost + ":" + proxyPort, clientChannel.getConnectionException()));
            }
        }
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
        if (!isClosed.compareAndExchange(false, true))
        {
            clientChannel.pipeline().shutdownInput();
        }
    }
}

