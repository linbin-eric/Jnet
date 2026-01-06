package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.StringUtil;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
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
//        log.trace("[AbstractIOResourceHandler] process: {}", part.getClass().getSimpleName());
        // IO 资源处理器只处理 Head，忽略 Body
        if (part instanceof HttpRequestPartHead head)
        {
            String requestUrl = URLDecoder.decode(head.getPath(), StandardCharsets.UTF_8);
            requestUrl = HttpDecodeUtil.pureUrl(requestUrl);
            // 检查是否匹配前缀
            if (!requestUrl.startsWith(prefixMatch))
            {
//                log.trace("[AbstractIOResourceHandler] 前缀不匹配: url={}, prefixMatch={}", requestUrl, prefixMatch);
                return false; // 不处理
            }
//            log.trace("[AbstractIOResourceHandler] 前缀匹配: url={}, prefixMatch={}", requestUrl, prefixMatch);
            // 截取资源路径
            requestUrl = requestUrl.substring(len);
            if (StringUtil.isBlank(requestUrl))
            {
                requestUrl = "index.html";
//                log.trace("[AbstractIOResourceHandler] 空路径默认为 index.html");
            }
            else if (requestUrl.equals("/"))
            {
                requestUrl = "/index.html";
//                log.trace("[AbstractIOResourceHandler] 根路径默认为 /index.html");
            }
//            log.trace("[AbstractIOResourceHandler] 处理Head, 资源路径: {}", requestUrl);
            processHead(head, pipeline, requestUrl, HttpDecodeUtil.findContentType(requestUrl));
            return true; // 已处理
        }
        else
        {
            // Body 部分释放资源（包括 last=true 的 body）
//            log.trace("[AbstractIOResourceHandler] IO资源处理器忽略Body, 释放资源");
            part.close();
            return true; // 已处理(虽然只是释放)
        }
    }

    protected abstract void processHead(HttpRequestPartHead head, Pipeline pipeline, String requestUrl, String contentType);
}
