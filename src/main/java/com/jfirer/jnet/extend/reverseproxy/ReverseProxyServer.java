package com.jfirer.jnet.extend.reverseproxy;

import com.jfirer.baseutil.RuntimeJVM;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.extend.http.coder.CorsEncoder;
import com.jfirer.jnet.extend.http.coder.HttpRequestDecoder;
import com.jfirer.jnet.extend.http.coder.HttpRespEncoder;
import com.jfirer.jnet.extend.reverseproxy.api.ResourceConfig;
import com.jfirer.jnet.server.AioServer;

import java.util.function.Consumer;

public class ReverseProxyServer
{
    private          int              port;
    private volatile ResourceConfig[] configs;

    public ReverseProxyServer(int port, ResourceConfig[] configs)
    {
        this.port    = port;
        this.configs = configs;
    }

    public void start()
    {
        if (RuntimeJVM.getDirOfMainClass() == null)
        {
            throw new NullPointerException("Main Class not register");
        }
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setPort(port);
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
