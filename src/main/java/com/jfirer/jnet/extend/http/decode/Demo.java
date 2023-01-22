package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.server.AioServer;

public class Demo
{
    public static void main(String[] args)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setPort(81);
        AioServer aioServer = new AioServer(channelConfig, channelContext -> {
            Pipeline pipeline = channelContext.pipeline();
            pipeline.addReadProcessor(new HttpRequestDecoder(channelConfig.getAllocator()));
            pipeline.addReadProcessor((HttpRequest httpRequest, ReadProcessorNode next) -> {
                HttpResponse response = new HttpResponse();
                response.setBody("""
                                 {"name":"林斌","age":12}
                                 """);
                pipeline.fireWrite(response);
            });
            pipeline.addWriteProcessor(new HttpResponseEncoder(channelConfig.getAllocator()));
        });
        aioServer.start();
    }
}
