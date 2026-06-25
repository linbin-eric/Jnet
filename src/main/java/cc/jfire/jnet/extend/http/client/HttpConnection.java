package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.HeartBeat;
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

    public HttpConnection(String domain, int port, int secondsOfKeepAlive)
    {
        this(domain, port, new HttpClientConfig().setKeepAliveSeconds(secondsOfKeepAlive), false);
    }

    public HttpConnection(String domain, int port, int secondsOfKeepAlive, boolean ssl)
    {
        this(domain, port, new HttpClientConfig().setKeepAliveSeconds(secondsOfKeepAlive), ssl);
    }

    private SSLEngine buildSSLEngineAndBeginHandShake(String domain, int port)
    {
        try
        {
            TrustManager[] trustAllCerts = HttpClientConfig.TRUST_ANYONE;
            SSLContext     sslContext    = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, null);
            SSLEngine sslEngine = sslContext.createSSLEngine(domain, port);
            sslEngine.setUseClientMode(true);
            sslEngine.beginHandshake();
            return sslEngine;
        }
        catch (Throwable e)
        {
            ReflectUtil.throwException(e);
            return null;
        }
    }

    class ProcessResponseFuture implements ReadProcessor<HttpResponsePart>
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
                next.pipeline().shutdownInput();
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
    }

    public HttpConnection(String domain, int port, String proxyHost, int proxyPort, boolean ssl)
    {
        this(domain, port, proxyHost, proxyPort, ssl, 30, 30);
    }

    /**
     * 通过 HTTP 代理创建连接（支持 HTTP/HTTPS 两种模式）
     *
     * @param domain                     目标服务器域名
     * @param port                       目标服务器端口
     * @param proxyHost                  代理服务器主机名
     * @param proxyPort                  代理服务器端口
     * @param ssl                        是否使用 SSL（true 为 HTTPS 代理隧道模式，false 为 HTTP 直接代理模式）
     * @param keepAliveSeconds           Keep-Alive 时间（秒）
     * @param sslHandshakeTimeoutSeconds SSL 握手超时时间（秒）
     */
    public HttpConnection(String domain, int port, String proxyHost, int proxyPort, boolean ssl, int keepAliveSeconds, int sslHandshakeTimeoutSeconds)
    {
        this(domain, port, new HttpClientConfig().setProxyHost(proxyHost).setProxyPort(proxyPort).setKeepAliveSeconds(keepAliveSeconds).setSslHandshakeTimeoutSeconds(sslHandshakeTimeoutSeconds), ssl);
    }

    public HttpConnection(String domain, int port, HttpClientConfig config, boolean ssl)
    {
        ProxyType proxyType = config.getProxyType() == null ? ProxyType.HTTP : config.getProxyType();
        if (!config.hasProxy())
        {
            clientChannel = buildDirect(domain, port, ssl, config.getKeepAliveSeconds(), config.getSslHandshakeTimeoutSeconds());
        }
        else if (proxyType == ProxyType.SOCKS5)
        {
            clientChannel = buildSocks5Proxy(domain, port, ssl, config);
        }
        else
        {
            clientChannel = buildHttpProxy(domain, port, ssl, config);
        }
    }

    private ClientChannel buildDirect(String domain, int port, boolean ssl, int keepAliveSeconds, int sslHandshakeTimeoutSeconds)
    {
        ChannelConfig channelConfig = new ChannelConfig().setIp(domain).setPort(port);
        ClientChannel channel       = ClientChannel.newClient(channelConfig, pipeline -> {
            SSLEncoder sslEncoder = null;
            if (ssl)
            {
                SSLEngine        sslEngine  = buildSSLEngineAndBeginHandShake(domain, port);
                ClientSSLDecoder sslDecoder = new ClientSSLDecoder(sslEngine);
                pipeline.putPersistenceStore(ClientSSLDecoder.KEY, sslDecoder);
                sslEncoder = new SSLEncoder(sslEngine);
                pipeline.addReadProcessor(sslDecoder);
            }
            pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
            pipeline.addReadProcessor(new HttpResponsePartDecoder());
            pipeline.addReadProcessor(new ProcessResponseFuture());
            pipeline.addWriteProcessor(new HttpRequestPartEncoder());
            pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
            if (sslEncoder != null)
            {
                pipeline.addWriteProcessor(sslEncoder);
            }
        });
        if (!channel.connect())
        {
            ReflectUtil.throwException(new RuntimeException("无法连接 " + domain + ":" + port, channel.getConnectionException()));
        }
        if (ssl)
        {
            waitSslHandshake(channel, sslHandshakeTimeoutSeconds);
        }
        return channel;
    }

    private ClientChannel buildHttpProxy(String domain, int port, boolean ssl, HttpClientConfig config)
    {
        String proxyHost                  = config.getProxyHost();
        int    proxyPort                  = config.getProxyPort();
        int    keepAliveSeconds           = config.getKeepAliveSeconds();
        int    sslHandshakeTimeoutSeconds = config.getSslHandshakeTimeoutSeconds();
        if (ssl)
        {
            ChannelConfig          channelConfig     = new ChannelConfig().setIp(proxyHost).setPort(proxyPort);
            ProxyTunnelReadHandler tunnelReadHandler = new ProxyTunnelReadHandler(domain, port);
            ClientChannel channel = ClientChannel.newClient(channelConfig, pipeline -> {
                try
                {
                    pipeline.addReadProcessor(tunnelReadHandler);
                    pipeline.putPersistenceStore(ProxyTunnelReadHandler.KEY, tunnelReadHandler);
                    SSLEngine        sslEngine  = buildSSLEngineAndBeginHandShake(domain, port);
                    ClientSSLDecoder sslDecoder = new ClientSSLDecoder(sslEngine);
                    pipeline.putPersistenceStore(ClientSSLDecoder.KEY, sslDecoder);
                    SSLEncoder sslEncoder = new SSLEncoder(sslEngine);
                    pipeline.addReadProcessor(sslDecoder);
                    pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                    pipeline.addReadProcessor(new HttpResponsePartDecoder());
                    pipeline.addReadProcessor(new ProcessResponseFuture());
                    pipeline.addWriteProcessor(new HttpRequestPartEncoder());
                    pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                    pipeline.addWriteProcessor(sslEncoder);
                }
                catch (Exception e)
                {
                    tunnelReadHandler.setTunnelError(e);
                }
            });
            connectProxyChannel(channel, "代理服务器", proxyHost, proxyPort);
            waitHttpProxyTunnel(channel, tunnelReadHandler, sslHandshakeTimeoutSeconds);
            waitSslHandshake(channel, sslHandshakeTimeoutSeconds);
            return channel;
        }
        else
        {
            ChannelConfig channelConfig = new ChannelConfig().setIp(proxyHost).setPort(proxyPort);
            ClientChannel channel = ClientChannel.newClient(channelConfig, pipeline -> {
                pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                pipeline.addReadProcessor(new HttpResponsePartDecoder());
                pipeline.addReadProcessor(new ProcessResponseFuture());
                pipeline.addWriteProcessor(new ProxyHttpRequestEncoder(domain, port));
                pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
            });
            connectProxyChannel(channel, "代理服务器", proxyHost, proxyPort);
            return channel;
        }
    }

    private ClientChannel buildSocks5Proxy(String domain, int port, boolean ssl, HttpClientConfig config)
    {
        String                  proxyHost                  = config.getProxyHost();
        int                     proxyPort                  = config.getProxyPort();
        int                     keepAliveSeconds           = config.getKeepAliveSeconds();
        int                     sslHandshakeTimeoutSeconds = config.getSslHandshakeTimeoutSeconds();
        ChannelConfig           channelConfig              = new ChannelConfig().setIp(proxyHost).setPort(proxyPort);
        Socks5TunnelReadHandler tunnelReadHandler          = new Socks5TunnelReadHandler(domain, port, config.getProxyUsername(), config.getProxyPassword());
        ClientChannel channel = ClientChannel.newClient(channelConfig, pipeline -> {
            try
            {
                pipeline.addReadProcessor(tunnelReadHandler);
                pipeline.putPersistenceStore(Socks5TunnelReadHandler.KEY, tunnelReadHandler);
                SSLEncoder sslEncoder = null;
                if (ssl)
                {
                    SSLEngine        sslEngine  = buildSSLEngineAndBeginHandShake(domain, port);
                    ClientSSLDecoder sslDecoder = new ClientSSLDecoder(sslEngine);
                    pipeline.putPersistenceStore(ClientSSLDecoder.KEY, sslDecoder);
                    sslEncoder = new SSLEncoder(sslEngine);
                    pipeline.addReadProcessor(sslDecoder);
                }
                pipeline.addReadProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                pipeline.addReadProcessor(new HttpResponsePartDecoder());
                pipeline.addReadProcessor(new ProcessResponseFuture());
                pipeline.addWriteProcessor(new HttpRequestPartEncoder());
                pipeline.addWriteProcessor(new HeartBeat(keepAliveSeconds, pipeline));
                if (sslEncoder != null)
                {
                    pipeline.addWriteProcessor(sslEncoder);
                }
            }
            catch (Exception e)
            {
                tunnelReadHandler.setTunnelError(e);
            }
        });
        connectProxyChannel(channel, "SOCKS5 代理服务器", proxyHost, proxyPort);
        waitSocks5Tunnel(channel, tunnelReadHandler, sslHandshakeTimeoutSeconds);
        if (ssl)
        {
            waitSslHandshake(channel, sslHandshakeTimeoutSeconds);
        }
        return channel;
    }

    private void connectProxyChannel(ClientChannel channel, String proxyName, String proxyHost, int proxyPort)
    {
        if (!channel.connect())
        {
            ReflectUtil.throwException(new RuntimeException("无法连接到" + proxyName + " " + proxyHost + ":" + proxyPort, channel.getConnectionException()));
        }
    }

    private void waitHttpProxyTunnel(ClientChannel channel, ProxyTunnelReadHandler tunnelReadHandler, int timeoutSeconds)
    {
        try
        {
            if (!tunnelReadHandler.awaitTunnelEstablished(timeoutSeconds, TimeUnit.SECONDS))
            {
                channel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("代理隧道建立超时"));
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("代理连接被中断", e));
        }
        if (tunnelReadHandler.getTunnelError() != null)
        {
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("代理隧道建立失败", tunnelReadHandler.getTunnelError()));
        }
        if (!tunnelReadHandler.isTunnelEstablished())
        {
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("代理服务器拒绝 CONNECT 请求"));
        }
    }

    private void waitSocks5Tunnel(ClientChannel channel, Socks5TunnelReadHandler tunnelReadHandler, int timeoutSeconds)
    {
        try
        {
            if (!tunnelReadHandler.awaitTunnelEstablished(timeoutSeconds, TimeUnit.SECONDS))
            {
                channel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("SOCKS5 代理隧道建立超时"));
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("SOCKS5 代理连接被中断", e));
        }
        if (tunnelReadHandler.getTunnelError() != null)
        {
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("SOCKS5 代理隧道建立失败", tunnelReadHandler.getTunnelError()));
        }
        if (!tunnelReadHandler.isTunnelEstablished())
        {
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("SOCKS5 代理服务器拒绝 CONNECT 请求"));
        }
    }

    private void waitSslHandshake(ClientChannel channel, int timeoutSeconds)
    {
        ClientSSLDecoder sslDecoder = (ClientSSLDecoder) channel.pipeline().getPersistenceStore(ClientSSLDecoder.KEY);
        try
        {
            if (!sslDecoder.waitHandshake(timeoutSeconds, TimeUnit.SECONDS))
            {
                channel.pipeline().shutdownInput();
                ReflectUtil.throwException(new RuntimeException("SSL 握手超时"));
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            channel.pipeline().shutdownInput();
            ReflectUtil.throwException(new RuntimeException("SSL 握手被中断", e));
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
        if (clientChannel == null)
        {
            isClosed.set(true);
            return;
        }
        if (!isClosed.compareAndExchange(false, true))
        {
            clientChannel.pipeline().shutdownInput();
        }
    }
}
