package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartDecoder;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import cc.jfire.jnet.extend.websocket.util.WebSocketHandshakeUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * 支持 WebSocket 升级的 HTTP 请求解码器。
 * 继承 HttpRequestPartDecoder，在检测到 WebSocket 握手请求后：
 * 1. 自动发送 101 Switching Protocols 响应
 * 2. 不向后传递 HTTP 请求头
 * 3. 切换到透传模式，后续数据作为 IoBuffer 直接传递给下一个处理器（WebSocketFrameDecoder）
 */
@Slf4j
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
        // 检查是否是 WebSocket 握手请求
        if (WebSocketHandshakeUtil.isWebSocketUpgrade(head))
        {
            // 发送 101 Switching Protocols 响应
            IoBuffer upgradeResponse = WebSocketHandshakeUtil.buildUpgradeResponse(head, next.pipeline().allocator());
            next.pipeline().fireWrite(upgradeResponse);
            // 释放请求头资源
            head.close();
            // 进入 WebSocket 透传模式
            webSocketMode = true;
            log.debug("WebSocket upgrade completed, switching to passthrough mode");
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
