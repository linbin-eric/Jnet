package com.jfirer.jnet.extend.reverse.proxy;

import com.jfirer.baseutil.RuntimeJVM;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.extend.http.coder.*;
import com.jfirer.jnet.extend.reverse.proxy.api.ResourceConfig;
import com.jfirer.jnet.server.AioServer;
import lombok.SneakyThrows;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.List;
import java.util.function.Consumer;

public class ReverseProxyServer
{
    private int                  port;
    private List<ResourceConfig> configs;
    private String               cert;
    private boolean              ssl = false;

    public ReverseProxyServer(int port, List<ResourceConfig> configs)
    {
        this.port    = port;
        this.configs = configs;
    }

    public ReverseProxyServer(int port, List<ResourceConfig> configs, String cert)
    {
        this.port    = port;
        this.configs = configs;
        this.cert    = cert;
        ssl          = true;
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
        if (ssl)
        {
            // 1. 加载 JKS 文件
            // 5. 创建 SSLEngine
            Consumer<Pipeline> s = pipeline -> {
                try
                {
                    KeyStore keyStore = KeyStore.getInstance("JKS");
                    File     file     = new File(RuntimeJVM.getDirOfMainClass(), cert);
                    System.out.println(file.getAbsolutePath());
                    if (file.exists() == false)
                    {
                        throw new IllegalArgumentException();
                    }
                    try (FileInputStream fis = new FileInputStream(file))
                    {
                        keyStore.load(fis, "123456".toCharArray());
                    }
                    // 2. 初始化 KeyManagerFactory
                    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                    kmf.init(keyStore, "123456".toCharArray());
                    // 3. 初始化 TrustManagerFactory（对于服务器端，通常需要信任客户端证书；客户端需要信任服务器证书）
                    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                    tmf.init(keyStore);
                    // 4. 初始化 SSLContext
                    SSLContext sslContext = SSLContext.getInstance("TLS");
                    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
                    SSLEngine sslEngine = sslContext.createSSLEngine();
                    sslEngine.setUseClientMode(false); // 设置客户端或服务器模式
                    sslEngine.setNeedClientAuth(false);
                    sslEngine.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                    try
                    {
                        sslEngine.beginHandshake();
                    }
                    catch (SSLException e)
                    {
                        throw new RuntimeException(e);
                    }
                    SSLEncoder sslEncoder = new SSLEncoder();
                    pipeline.addReadProcessor(new SSLDecoder(sslEngine, sslEncoder));
                    pipeline.addReadProcessor(new HttpRequestDecoder());
                    pipeline.addReadProcessor(new TransferProcessor(configs));
                    pipeline.addWriteProcessor(new CorsEncoder());
                    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
                    pipeline.addWriteProcessor(sslEncoder);
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
                pipeline.addReadProcessor(new HttpRequestDecoder());
                pipeline.addReadProcessor(new TransferProcessor(configs));
                pipeline.addWriteProcessor(new CorsEncoder());
                pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
            };
            AioServer aioServer = AioServer.newAioServer(channelConfig, s::accept);
            aioServer.start();
        }
    }
}
