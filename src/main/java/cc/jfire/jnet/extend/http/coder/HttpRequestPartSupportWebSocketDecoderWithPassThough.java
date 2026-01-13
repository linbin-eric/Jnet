package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

public class HttpRequestPartSupportWebSocketDecoderWithPassThough extends HttpRequestPartDecoder
{
    private boolean webSocketMode = false;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        // 如果已进入 WebSocket 透传模式，直接透传数据
        if (webSocketMode)
        {
            if (accumulation != null && accumulation.remainRead() > 0)
            {
                IoBuffer data = accumulation;
                accumulation = null;
                next.fireRead(data);
            }
            return;
        }
        // 否则使用父类的 HTTP 解析逻辑
        super.process0(next);
    }

    @Override
    protected boolean doProcessRequestHead(ReadProcessorNode next, HttpRequestPartHead head)
    {
        // 检查是否是 WebSocket 握手请求（header name 已规范化为 Upgrade）
        String upgrade = head.getHeaders().get("Upgrade");
        if ("websocket".equalsIgnoreCase(upgrade))
        {
            head.setWebSocketUpgrade(true);
            webSocketMode = true;
        }
        return super.doProcessRequestHead(next, head);
    }
}
