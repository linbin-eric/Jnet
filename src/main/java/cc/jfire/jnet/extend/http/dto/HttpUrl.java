package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.util.ReflectUtil;

public record HttpUrl(String domain, int port, String path, String hostHeader, boolean ssl)
{
    public static HttpUrl parse(String url)
    {
        try
        {
            int     index       = 0;
            int     domainStart = 0;
            boolean isHttps     = false;
            if (url.startsWith("http://"))
            {
                index       = url.indexOf("/", 8);
                domainStart = 7;
            }
            else if (url.startsWith("https://"))
            {
                index       = url.indexOf("/", 9);
                domainStart = 8;
                isHttps     = true;
            }
            // 保持与原 HttpRequest#setUrl 一致：若不是 http/https 开头，则按旧逻辑继续解析（domainStart/index 默认为 0）
            if (index == -1)
            {
                index = url.length();
            }
            int portStart = url.indexOf(':', domainStart);
            if (portStart > index)
            {
                portStart = -1;
            }
            String path       = index == url.length() ? "/" : url.substring(index);
            int    port       = portStart == -1 ? (isHttps ? 443 : 80) : Integer.parseInt(url.substring(portStart + 1, index));
            String domain     = portStart == -1 ? url.substring(domainStart, index) : url.substring(domainStart, portStart);
            String hostHeader = portStart == -1 ? domain : url.substring(domainStart, index);
            return new HttpUrl(domain, port, path, hostHeader, isHttps);
        }
        catch (Throwable e)
        {
//            log.error("出现未知异常，url 是:{}", url,e);
            ReflectUtil.throwException(e);
            return null;
        }
    }
}

