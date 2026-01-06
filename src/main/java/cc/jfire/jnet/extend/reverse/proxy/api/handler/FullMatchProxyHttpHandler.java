package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import lombok.Getter;

/**
 * 请求url完全匹配match，则将请求发向proxy
 */
@Getter
public final class FullMatchProxyHttpHandler extends ProxyHttpHandler
{
    private final String match;
    private final String proxy;

    public FullMatchProxyHttpHandler(String match, String proxy, HttpConnection2Pool pool)
    {
        super(pool);
        this.match = match;
        this.proxy = proxy;
    }

    @Override
    protected boolean matchRequest(HttpRequestPartHead head)
    {
        String requestUrl = head.getPath();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        index = requestUrl.indexOf("?");
        String matchPart = index == -1 ? requestUrl : requestUrl.substring(0, index);
        return matchPart.equals(match);
    }

    @Override
    protected void computeBackendUrl(HttpRequestPartHead head)
    {
        String requestUrl = head.getPath();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        index = requestUrl.indexOf("?");
        String backendUrl = index == -1 ? proxy : proxy + requestUrl.substring(index);

        // 统一使用 HttpRequestPartHead#setUrl 解析并更新 path/Host，同时释放/清空 headBuffer（避免旧头被直接写出）
        head.setUrl(backendUrl);
    }
}

