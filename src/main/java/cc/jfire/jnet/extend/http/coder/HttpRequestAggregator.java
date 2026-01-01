package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.*;

public class HttpRequestAggregator implements ReadProcessor<HttpRequestPart>
{
    private HttpRequestPartHead head;
    private IoBuffer            body;

    @Override
    public void read(HttpRequestPart data, ReadProcessorNode next)
    {
        if (data instanceof HttpRequestPartHead)
        {
            head = (HttpRequestPartHead) data;
        }
        else if (data instanceof HttpRequestPartBodyPart)
        {
            HttpRequestPartBodyPart part = (HttpRequestPartBodyPart) data;
            if (body == null)
            {
                body = part.getPart();
            }
            else
            {
                body.put(part.getPart());
                part.getPart().free();
            }
        }
        else if (data instanceof HttpRequestPartEnd)
        {
            HttpRequest request = new HttpRequest();
            request.setMethod(head.getMethod());
            request.setUrl(head.getUrl());
            request.setVersion(head.getVersion());
            request.setHeaders(head.getHeaders());
            request.setContentLength(head.getContentLength());
            HttpDecodeUtil.findContentType(head.getHeaders(), request::setContentType);
            request.setBody(body);
            next.fireRead(request);
            head = null;
            body = null;
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (body != null)
        {
            body.free();
            body = null;
        }
        head = null;
        next.fireReadFailed(e);
    }
}
