package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRespHead;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class CorsEncoder implements WriteProcessor<Object>
{
    @Override
    public void write(Object data, WriteProcessorNode next)
    {
//        log.trace("[CorsEncoder] write: {}", data.getClass().getSimpleName());
        if (data instanceof FullHttpResp fullHttpResp)
        {
//            log.trace("[CorsEncoder] 处理FullHttpResp, 添加CORS头");
            HttpRespHead head = fullHttpResp.getHead();
            head.addHeader("Access-Control-Allow-Origin", "*")
                .addHeader("access-control-allow-methods", "GET,PUT,POST,HEAD")
                .addHeader("Access-Control-Max-Age", "86400")
                .addHeader("Access-Control-Allow-Headers", "*");
            next.fireWrite(fullHttpResp);
        }
        else if (data instanceof HttpResponsePartHead head)
        {
//            log.trace("[CorsEncoder] 处理HttpResponsePartHead, statusCode: {}", head.getStatusCode());
//            // 释放并清空旧的 part，避免编码器写出原始字节
//            IoBuffer old = head.getPart();
//            head.setPart(null);
//            if (old != null)
//            {
//                log.trace("[CorsEncoder] 释放旧的part buffer");
//                old.free();
//            }
//            // 大小写不敏感地补充 CORS 头
//            addCorsHeadersIgnoreCase(head.getHeaders());
//            log.trace("[CorsEncoder] CORS头已添加, headers: {}", head.getHeaders());
            next.fireWrite(head);
        }
        else
        {
//            log.trace("[CorsEncoder] 透传数据: {}", data.getClass().getSimpleName());
            next.fireWrite(data);
        }
    }

    private void addCorsHeadersIgnoreCase(Map<String, String> headers)
    {
        if (!containsIgnoreCase(headers, "Access-Control-Allow-Origin"))
        {
            headers.put("Access-Control-Allow-Origin", "*");
        }
        if (!containsIgnoreCase(headers, "Access-Control-Allow-Methods"))
        {
            headers.put("Access-Control-Allow-Methods", "GET,PUT,POST,HEAD");
        }
        if (!containsIgnoreCase(headers, "Access-Control-Max-Age"))
        {
            headers.put("Access-Control-Max-Age", "86400");
        }
        if (!containsIgnoreCase(headers, "Access-Control-Allow-Headers"))
        {
            headers.put("Access-Control-Allow-Headers", "*");
        }
    }

    private boolean containsIgnoreCase(Map<String, String> headers, String key)
    {
        return headers.keySet().stream().anyMatch(k -> k.equalsIgnoreCase(key));
    }
}

