package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartDecoder;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import cc.jfire.jnet.extend.websocket.dto.WebSocketAttachHttpRequest;
import cc.jfire.jnet.extend.websocket.util.WebSocketHandshakeUtil;

public class WebSocketUpgradeDecoder extends HttpRequestPartDecoder
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
                next.fireRead(accumulation);
                accumulation = null;
            }
            return;
        }
        // 否则使用父类的 HTTP 解析逻辑
        super.process0(next);
    }

    @Override
    protected boolean doProcessRequestHead(ReadProcessorNode next, HttpRequestPartHead head)
    {
        // 检查是否是 WebSocket 握手请求
        if (WebSocketHandshakeUtil.isWebSocketUpgrade(head))
        {
            webSocketMode = true;
            next.fireRead(new WebSocketAttachHttpRequest(head));
            // 返回是否需要继续处理
            if (accumulation == null)
            {
                return false;
            }
            else
            {
                accumulation.compact();
                return true;
            }
        }
        // 非 WebSocket 请求，调用父类默认处理
        return super.doProcessRequestHead(next, head);
    }
}
