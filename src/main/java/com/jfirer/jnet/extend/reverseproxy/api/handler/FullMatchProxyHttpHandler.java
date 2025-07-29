package com.jfirer.jnet.extend.reverseproxy.api.handler;

import com.jfirer.jnet.common.api.Pipeline;
import com.jfirer.jnet.extend.http.dto.HttpRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 请求url完全匹配match，则将请求发向proxy
 */
@EqualsAndHashCode(callSuper = true)
@Data
public final class FullMatchProxyHttpHandler extends ProxyHttpHandler
{
    private final String match;
    private final String proxy;

    @Override
    public boolean process(HttpRequest request, Pipeline pipeline)
    {
        String requestUrl = request.getUrl();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        index = requestUrl.indexOf("?");
        String matchPart = index == -1 ? requestUrl : requestUrl.substring(0, index);
        if (matchPart.equals(match))
        {
            proxyBackendUrl(request, pipeline, index == -1 ? proxy : proxy + requestUrl.substring(index));
            return true;
        }
        else
        {
            return false;
        }
    }
}
