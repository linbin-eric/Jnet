package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRespHead;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;

import java.util.Map;

public class CorsEncoder implements WriteProcessor<Object>
{
    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (data instanceof FullHttpResp fullHttpResp)
        {
            HttpRespHead head = fullHttpResp.getHead();
            head.addHeader("Access-Control-Allow-Origin", "*")
                .addHeader("access-control-allow-methods", "GET,PUT,POST,HEAD")
                .addHeader("Access-Control-Max-Age", "86400")
                .addHeader("Access-Control-Allow-Headers", "*");
            next.fireWrite(fullHttpResp);
        }
        else if (data instanceof HttpResponsePartHead head)
        {
            // 释放并清空旧的 part，避免编码器写出原始字节
            IoBuffer old = head.getPart();
            head.setPart(null);
            if (old != null)
            {
                old.free();
            }
            // 大小写不敏感地补充 CORS 头
            addCorsHeadersIgnoreCase(head.getHeaders());
            next.fireWrite(head);
        }
        else
        {
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

