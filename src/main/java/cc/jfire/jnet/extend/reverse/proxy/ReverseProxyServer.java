package cc.jfire.jnet.extend.reverse.proxy;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.RuntimeJVM;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.internal.DefaultPipeline;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.reverse.app.SslInfo;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.watercheck.BackPresure;
import cc.jfire.jnet.server.AioServer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class ReverseProxyServer
{
    private int                  port;
    private List<ResourceConfig> configs;
    private SslInfo              sslInfo;
    private HttpConnectionPool   pool = new HttpConnectionPool();

    public ReverseProxyServer(int port, List<ResourceConfig> configs)
    {
        this.port    = port;
        this.configs = configs;
        this.sslInfo = new SslInfo().setEnable(false);
    }

    public ReverseProxyServer(int port, List<ResourceConfig> configs, SslInfo sslInfo)
    {
        this.port    = port;
        this.configs = configs;
        this.sslInfo = sslInfo;
    }

    @SneakyThrows
    public void start()
    {
        if (RuntimeJVM.getDirOfMainClass() == null)
        {
            throw new NullPointerException("Main Class not register");
        }
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setPort(port);
        BackPresure inBackPresure       = BackPresure.noticeWaterLevel(1024 * 1024 * 100);
        BackPresure upstreamBackPresure = BackPresure.noticeWaterLevel(1024 * 1024 * 100);
        if (sslInfo.isEnable())
        {
            // 1. 加载 JKS 文件
            // 5. 创建 SSLEngine
            Consumer<Pipeline> s = pipeline -> {
                try
                {
                    ((DefaultPipeline) pipeline).putPersistenceStore(BackPresure.UP_STREAM_BACKPRESURE, upstreamBackPresure);
                    ((DefaultPipeline) pipeline).putPersistenceStore(BackPresure.IN_BACKPRESURE, inBackPresure);
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    String   filePath = sslInfo.getCert();
                    if (IoUtil.isFilePathAbsolute(filePath))
                    {
                        try (FileInputStream fileInputStream = new FileInputStream(filePath))
                        {
                            keyStore.load(fileInputStream, sslInfo.getPassword().toCharArray());
                        }
                    }
                    else
                    {
                        try (FileInputStream fileInputStream = new FileInputStream(new File(RuntimeJVM.getDirOfMainClass(), filePath)))
                        {
                            keyStore.load(fileInputStream, sslInfo.getPassword().toCharArray());
                        }
                    }
                    // 2. 初始化 KeyManagerFactory
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keyStore, sslInfo.getPassword().toCharArray());
                    // 3. 初始化 TrustManagerFactory（对于服务器端，通常需要信任客户端证书；客户端需要信任服务器证书）
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keyStore);
                    // 4. 初始化 SSLContext
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(kmf.getKeyManagers(), null, null);
                    SSLEngine sslEngine = sslContext.createSSLEngine();
                    sslEngine.setUseClientMode(false); // 设置客户端或服务器模式
                    sslEngine.setNeedClientAuth(false);
                    sslEngine.setEnabledCipherSuites(sslEngine.getSupportedCipherSuites());
                    sslEngine.setEnabledProtocols(sslEngine.getSupportedProtocols()); // 启用现代协议
                    try
                    {
                        sslEngine.beginHandshake();
                    }
                    catch (SSLException e)
                    {
                        throw new RuntimeException(e);
                    }
                    SSLDecoder sslDecoder = new SSLDecoder(sslEngine);
                    SSLEncoder sslEncoder = new SSLEncoder(sslEngine, sslDecoder);
                    pipeline.addReadProcessor(sslDecoder);
                    pipeline.addReadProcessor(new HttpRequestPartSupportWebSocketDecoder());
                    pipeline.addReadProcessor(new TransferProcessor(configs, pool));
                    pipeline.addReadProcessor(inBackPresure.readLimiter());
                    pipeline.addWriteProcessor(new CorsEncoder());
                    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
                    pipeline.addWriteProcessor(sslEncoder);
                    pipeline.setWriteListener(upstreamBackPresure.writeLimiter());
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
            };
            AioServer aioServer = AioServer.newAioServer(channelConfig, s::accept);
            aioServer.start();
        }
        else
        {
            Consumer<Pipeline> s = pipeline -> {
                ((DefaultPipeline) pipeline).putPersistenceStore(BackPresure.UP_STREAM_BACKPRESURE, upstreamBackPresure);
                ((DefaultPipeline) pipeline).putPersistenceStore(BackPresure.IN_BACKPRESURE, inBackPresure);
                pipeline.addReadProcessor(new HttpRequestPartDecoder());
                pipeline.addReadProcessor(new TransferProcessor(configs, pool));
                pipeline.addReadProcessor(inBackPresure.readLimiter());
                pipeline.addWriteProcessor(new CorsEncoder());
                pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
                pipeline.setWriteListener(upstreamBackPresure.writeLimiter());
            };
            AioServer aioServer = AioServer.newAioServer(channelConfig, s::accept);
            aioServer.start();
        }
    }
}
