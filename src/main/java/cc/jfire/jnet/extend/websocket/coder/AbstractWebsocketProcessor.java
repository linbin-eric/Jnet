package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import cc.jfire.jnet.extend.websocket.dto.WebSocketFrame;
import cc.jfire.jnet.extend.websocket.util.WebSocketHandshakeUtil;

public abstract class AbstractWebsocketProcessor implements ReadProcessor<Object>
{
    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        if (data instanceof WebSocketAttachHttpRequest websocketAttachHttpRequest)
        {
            responseProtocol_101(next, websocketAttachHttpRequest);
            processWebSocketAttachHttpRequest(websocketAttachHttpRequest, next.pipeline());
        }
        else if (data instanceof WebSocketFrame webSocketFrame)
        {
            processWebSocketFrame(webSocketFrame, next);
        }
        else
        {
            next.fireRead(data);
        }
    }

    /**
     * 发送 101 Switching Protocols 响应
     * @param next
     * @param websocketAttachHttpRequest
     */
    private void responseProtocol_101(ReadProcessorNode next, WebSocketAttachHttpRequest websocketAttachHttpRequest)
    {
        HttpRequestPartHead head            = websocketAttachHttpRequest.head();
        IoBuffer            upgradeResponse = WebSocketHandshakeUtil.buildUpgradeResponse(head, next.pipeline().allocator());
        next.pipeline().fireWrite(upgradeResponse);
    }

    protected void processWebSocketAttachHttpRequest(WebSocketAttachHttpRequest data, Pipeline pipeline)
    {
        data.head().close();
    }

    protected abstract void processWebSocketFrame(WebSocketFrame data, ReadProcessorNode next);
}


