package com.jfirer.jnet;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.allocator.impl.PooledBufferAllocator;
import com.jfirer.jnet.common.util.ChannelConfig;
import com.jfirer.jnet.extend.http.decode.*;
import com.jfirer.jnet.server.DefaultAioServer;

import java.io.FileInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DownloadServerDemo
{
    public static void main(String[] args)
    {
        ChannelConfig channelConfig = new ChannelConfig();
        channelConfig.setPort(81);
        DefaultAioServer aioServer = new DefaultAioServer(channelConfig, pipeline -> {
            pipeline.addReadProcessor(new HttpRequestDecoder(channelConfig.getAllocator()));
            pipeline.addReadProcessor((HttpRequest httpRequest, ReadProcessorNode next) -> {
                try
                {
                    HttpResponse response = new HttpResponse();
                    response.setContentType(ContentType.STREAM);
                    response.getHeaders().put(ContentType.DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode("国家医疗保障疾病诊断相关分组（CHS-DRG）细分组（1.0版）.pdf", StandardCharsets.UTF_8));
                    FileInputStream fileInputStream = new FileInputStream("/Users/linbin/SynologyDrive/附件/357/参考材料/技术规范类/国家医疗保障疾病诊断相关分组（CHS-DRG）细分组（1.0版）.pdf");
                    response.setBodyBuffer(PooledBufferAllocator.DEFAULT.ioBuffer(1024).put(fileInputStream.readAllBytes()));
                    pipeline.fireWrite(response);
                }
                catch (Throwable e)
                {
                    e.printStackTrace();
                }
            });
            pipeline.addWriteProcessor(new HttpResponseEncoder(channelConfig.getAllocator()));
        });
        aioServer.start();
    }
}
