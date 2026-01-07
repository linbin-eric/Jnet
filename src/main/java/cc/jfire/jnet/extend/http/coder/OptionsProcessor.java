package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequest;

public class OptionsProcessor implements ReadProcessor<HttpRequest>
{
    @Override
    public void read(HttpRequest request, ReadProcessorNode next)
    {
        if (request.getHead().getMethod().equalsIgnoreCase("options"))
        {
            FullHttpResponse response = new FullHttpResponse();
            next.pipeline().fireWrite(response);
        }
        else
        {
            String url = request.getHead().getPath();
            if (url.equals("/favicon.ico") || url.equals("/robots.txt"))
            {
                FullHttpResponse response = new FullHttpResponse();
                response.getHead().setStatusCode(404);
                response.getHead().setReasonPhrase("Not Found");
                next.pipeline().fireWrite(response);
            }
            else
            {
                next.fireRead(request);
            }
        }
    }
}
