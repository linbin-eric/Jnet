package cc.jfire.jnet;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.YamlReader;
import cc.jfire.jnet.common.api.PipelineInitializer;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.http.coder.CorsEncoder;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartDecoder;
import cc.jfire.jnet.extend.http.coder.HttpRespEncoder;
import cc.jfire.jnet.extend.reverse.app.SslInfo;
import cc.jfire.jnet.extend.reverse.proxy.TransferProcessor;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.server.AioServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

@Slf4j
public class TestReverseApp
{
    @Test
    public void test() throws URISyntaxException
    {
        String uri = TestReverseApp.class.getProtectionDomain().getCodeSource().getLocation().toURI().toString();
        String filePath;
        if (uri.contains("!/"))
        {
            filePath = uri.substring(5, uri.indexOf("!/"));
        }
        else
        {
            filePath = uri.substring(5);
        }
        try
        {
            filePath = URLDecoder.decode(filePath, "UTF-8");
        }
        catch (UnsupportedEncodingException var4)
        {
            log.warn("URL解码失败，使用原始路径: {}", filePath);
        }
        File file = new File(new File(filePath).getParentFile().getParentFile(), "reverse.config");
        try (FileInputStream fileInputStream = new FileInputStream(file))
        {
            byte[]              bytes                  = IoUtil.readAllBytes(fileInputStream);
            String              s                      = new String(bytes, StandardCharsets.UTF_8);
            YamlReader          yamlReader             = new YamlReader(s);
            Map<String, Object> mapWithIndentStructure = yamlReader.getMapWithIndentStructure();
            for (Map.Entry<String, Object> entry : mapWithIndentStructure.entrySet())
            {
                String               port    = entry.getKey();
                List<ResourceConfig> list    = new ArrayList<>();
                Map<String, Object>  value   = (Map<String, Object>) entry.getValue();
                SslInfo              sslInfo = null;
                for (Map.Entry<String, Object> each : value.entrySet())
                {
                    String url = each.getKey();
                    if (url.equals("ssl"))
                    {
                        Map<String, String> sslConfig = (Map<String, String>) each.getValue();
                        sslInfo = new SslInfo()//
                                               .setEnable(Boolean.parseBoolean(sslConfig.get("enable")))//
                                               .setPassword(sslConfig.get("password"))//
                                               .setCert(sslConfig.get("cert"));
                        continue;
                    }
                    Map<String, String> value1 = (Map<String, String>) each.getValue();
                    String              type   = value1.get("type");
                    String              order  = value1.getOrDefault("order", "1");
                    String              path   = value1.get("path");
                    if (type.equals("resource"))
                    {
                        list.add(ResourceConfig.io(url, path, Integer.parseInt(order)));
                    }
                    else if (type.equals("proxy"))
                    {
                        if (url.endsWith("*"))
                        {
                            list.add(ResourceConfig.prefixMatch(url, path, Integer.parseInt(order)));
                        }
                        else
                        {
                            list.add(ResourceConfig.fullMatch(url, path, Integer.parseInt(order)));
                        }
                    }
                }
                HttpConnection2Pool pool = new HttpConnection2Pool();
                PipelineInitializer consumer = pipeline -> {
                    pipeline.addReadProcessor(new HttpRequestPartDecoder());
                    pipeline.addReadProcessor(new TransferProcessor(list, pool));
                    pipeline.addWriteProcessor(new CorsEncoder());
                    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
                };
                ChannelConfig channelConfig = new ChannelConfig();
                channelConfig.setPort(Integer.parseInt(port));
                AioServer aioServer = AioServer.newAioServer(channelConfig, consumer);
                aioServer.start();
            }
        }
        catch (Throwable e)
        {
            log.error("启动失败", e);
        }
        LockSupport.park();
    }
}
