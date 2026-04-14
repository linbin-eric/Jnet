package cc.jfire.jnet.extend.websocket.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.websocket.dto.WebSocketFrame;

public abstract class AbstractWebsocketProcessor implements ReadProcessor<Object>
{
    @Override
    public void read(Object data, ReadProcessorNode next)
    {
        if (data instanceof WebSocketAttachHttpRequest websocketAttachHttpRequest)
        {
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

    protected void processWebSocketAttachHttpRequest(WebSocketAttachHttpRequest data, Pipeline pipeline)
    {
        data.head().close();
    }

    protected abstract void processWebSocketFrame(WebSocketFrame data, ReadProcessorNode next);
}


