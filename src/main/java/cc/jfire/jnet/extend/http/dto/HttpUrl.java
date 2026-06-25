package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.util.ReflectUtil;

public record HttpUrl(String domain, int port, String path, String hostHeader, boolean ssl)
{
    public static HttpUrl parse(String url)
    {
        try
        {
            int     domainStart;
            boolean isHttps = false;
            if (url.startsWith("http://"))
            {
                domainStart = 7;
            }
            else if (url.startsWith("https://"))
            {
                domainStart = 8;
                isHttps = true;
            }
            else
            {
                domainStart = 0;
            }
            // 保持与原 HttpRequest#setUrl 一致：若不是 http/https 开头，则按旧逻辑继续解析（domainStart/index 默认为 0）
            int pathStart = url.indexOf("/", domainStart);
            if (pathStart == -1)
            {
                pathStart = url.length();
            }
            String authority  = url.substring(domainStart, pathStart);
            String path       = pathStart == url.length() ? "/" : url.substring(pathStart);
            String domain;
            String hostHeader;
            int    port;
            if (authority.startsWith("["))
            {
                int end = authority.indexOf(']');
                if (end == -1)
                {
                    throw new IllegalArgumentException("非法 IPv6 URL: " + url);
                }
                domain     = authority.substring(1, end);
                hostHeader = authority;
                if (authority.length() > end + 1)
                {
                    if (authority.charAt(end + 1) != ':')
                    {
                        throw new IllegalArgumentException("非法 IPv6 URL: " + url);
                    }
                    port = Integer.parseInt(authority.substring(end + 2));
                }
                else
                {
                    port = isHttps ? 443 : 80;
                }
            }
            else
            {
                int portStart = authority.lastIndexOf(':');
                if (portStart != -1)
                {
                    domain     = authority.substring(0, portStart);
                    port       = Integer.parseInt(authority.substring(portStart + 1));
                    hostHeader = authority;
                }
                else
                {
                    domain     = authority;
                    port       = isHttps ? 443 : 80;
                    hostHeader = authority;
                }
            }
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
