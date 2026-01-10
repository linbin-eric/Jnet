package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.StringUtil;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.util.HttpCoderUtil;
import cc.jfire.jnet.extend.http.dto.HttpRequestPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import lombok.extern.slf4j.Slf4j;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

@Slf4j
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
    public boolean process(HttpRequestPart part, Pipeline pipeline)
    {
        if (part instanceof HttpRequestPartHead head)
        {
            String requestUrl = URLDecoder.decode(head.getPath(), StandardCharsets.UTF_8);
            requestUrl = HttpCoderUtil.pureUrl(requestUrl);
            // 检查是否匹配前缀
            if (!requestUrl.startsWith(prefixMatch))
            {
                return false; // 不处理
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
            processHead(head, pipeline, requestUrl, HttpCoderUtil.findContentType(requestUrl));
        }
        else
        {
            part.close();
        }
        return true;
    }

    protected abstract void processHead(HttpRequestPartHead head, Pipeline pipeline, String requestUrl, String contentType);
}
