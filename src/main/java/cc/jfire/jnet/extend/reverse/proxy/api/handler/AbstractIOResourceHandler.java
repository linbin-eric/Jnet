package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.StringUtil;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public sealed abstract class AbstractIOResourceHandler implements ResourceHandler permits FileResourceHandler, ClassResourceHandler
{
    protected String prefixMatch;
    protected int    len;
    protected String path;

    /**
     * 通过matchUrl进行前缀匹配。
     * 匹配成功的情况下，截取地址中非prefixMatch的部分，拼接在path后，作为完整的资源地址进行读取
     *
     * @param prefixMatch
     * @param path
     */
    public AbstractIOResourceHandler(String prefixMatch, String path)
    {
        this.prefixMatch = prefixMatch;
        len              = prefixMatch.length();
        if (path.startsWith("file:"))
        {
            this.path = path.substring("file:".length());
        }
        else
        {
            this.path = path.substring("classpath:".length());
        }
    }

    @Override
    public boolean process(HttpRequest httpRequest, Pipeline pipeline)
    {
        String requestUrl = URLDecoder.decode(httpRequest.getPath(), StandardCharsets.UTF_8);
        requestUrl = HttpDecodeUtil.pureUrl(requestUrl);
        if (!requestUrl.startsWith(prefixMatch))
        {
            return false;
        }
        requestUrl = requestUrl.substring(len);
        if (StringUtil.isBlank(requestUrl))
        {
            requestUrl = "index.html";
        }
        else if (requestUrl.equals("/"))
        {
            requestUrl = "/index.html";
        }
        process(httpRequest, pipeline, requestUrl, HttpDecodeUtil.findContentType(requestUrl));
        return true;
    }

    protected abstract void process(HttpRequest httpRequest, Pipeline pipeline, String requestUrl, String contentType);
}
