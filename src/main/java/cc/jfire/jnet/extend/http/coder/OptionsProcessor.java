package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRequest;

public class OptionsProcessor implements ReadProcessor<HttpRequest>
{
    @Override
    public void read(HttpRequest request, ReadProcessorNode next)
    {
        if (request.getMethod().equalsIgnoreCase("options"))
        {
            FullHttpResp response = new FullHttpResp();
            next.pipeline().fireWrite(response);
        }
        else
        {
            String url = request.getUrl();
            if (url.equals("/favicon.ico") || url.equals("/robots.txt"))
            {
                FullHttpResp response = new FullHttpResp();
                response.getHead().setResponseCode(404);
                next.pipeline().fireWrite(response);
            }
            else
            {
                next.fireRead(request);
            }
        }
    }
}
