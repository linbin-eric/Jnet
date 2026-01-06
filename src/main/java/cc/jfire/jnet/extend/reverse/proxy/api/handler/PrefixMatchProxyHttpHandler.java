package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.STR;
import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

/**
 * 前缀匹配
 * 匹配规则:
 * 1. 匹配规则必须以"/*"结尾，且规则路径中只能有一个*
 * 2. 匹配规则里非*的部分是请求url的开始部分。
 * 3. 请求url中去掉匹配规则长度（不包含*）的部分拼接上proxy的部分，作为后端的url。
 */
public final class PrefixMatchProxyHttpHandler extends ProxyHttpHandler
{
    private final String prefixMatch;
    private final int    len;
    private final String proxy;

    public PrefixMatchProxyHttpHandler(String prefixMatch, String proxy, HttpConnection2Pool pool)
    {
        super(pool);
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
    protected boolean matchRequest(HttpRequestPartHead head)
    {
        String requestUrl = head.getPath();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        return requestUrl.startsWith(prefixMatch);
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
        String backendUrl = proxy + requestUrl.substring(len);

        // 统一使用 HttpRequestPartHead#setUrl 解析并更新 path/Host，同时释放/清空 headBuffer（避免旧头被直接写出）
        head.setUrl(backendUrl);
    }
}

