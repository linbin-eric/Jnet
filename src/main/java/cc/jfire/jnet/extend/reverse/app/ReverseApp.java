package cc.jfire.jnet.extend.reverse.app;

import cc.jfire.baseutil.IoUtil;
import cc.jfire.baseutil.RuntimeJVM;
import cc.jfire.baseutil.YamlReader;
import cc.jfire.jnet.extend.reverse.proxy.ReverseProxyServer;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
public class ReverseApp
{
    @SneakyThrows
    public static void main(String[] args)
    {
        RuntimeJVM.registerMainClass();
        File file = new File(RuntimeJVM.getDirOfMainClass(), "reverse.config");
        if (RuntimeJVM.detectRunningInJar())
        {
            RuntimeJVM.checkMainStart("ReverseApp", "ReverseApp-copy");
        }
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
                if (sslInfo != null && sslInfo.isEnable())
                {
                    new ReverseProxyServer(Integer.parseInt(port), list, sslInfo).start();
                }
                else
                {
                    new ReverseProxyServer(Integer.parseInt(port), list).start();
                }
            }
        }
        catch (Throwable e)
        {
            log.error("启动失败", e);
        }
    }
}
