package com.jfirer.jnet.extend.reverse.proxy.api.handler;

import com.jfirer.baseutil.STR;
import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.extend.http.dto.HttpRequest;

/**
 * 前缀匹配
 * 匹配规则:
 * 1. 匹配规则必须以"/*"结尾，且规则路径中只能有一个*
 * 2. 匹配规则里非*的部分是请求url的开始部分。
 * 3. 请求url中去掉匹配规则长度（不包含*）的部分拼接上proxy的部分，作为后端的url。
 */
public final class PrefixMatchProxyHttpHandler extends ProxyHttpHandler
{
    private String prefixMatch;
    private int    len;
    private String proxy;

    public PrefixMatchProxyHttpHandler(String prefixMatch, String proxy)
    {
        isValidPrefix(prefixMatch);
        this.prefixMatch = prefixMatch.substring(0, prefixMatch.length() - 1);
        len              = this.prefixMatch.length();
        this.proxy       = proxy;
    }

    private void isValidPrefix(String str)
    {
        if (str.endsWith("/*") && str.chars().filter(c -> c == '*').count() == 1)
        {
            ;
        }
        else
        {
            throw new IllegalArgumentException(STR.format("{}不是合规的前缀匹配地址", str));
        }
    }

    @Override
    public boolean process(HttpRequest request, Pipeline pipeline)
    {
        String requestUrl = request.getUrl();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        if (requestUrl.startsWith(prefixMatch))
        {
            String backendUrl = proxy + requestUrl.substring(len);
            proxyBackendUrl(request, pipeline, backendUrl);
            return true;
        }
        else
        {
            return false;
        }
    }
}
