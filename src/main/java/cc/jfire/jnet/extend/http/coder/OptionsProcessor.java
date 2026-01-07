package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequest;

public class OptionsProcessor implements ReadProcessor<HttpRequest>
{
    @Override
    public void read(HttpRequest request, ReadProcessorNode next)
    {
        if (request.getHead().getMethod().equalsIgnoreCase("options"))
        {
            HttpResponse response = new HttpResponse();
            next.pipeline().fireWrite(response);
        }
        else
        {
            String url = request.getHead().getPath();
            if (url.equals("/favicon.ico") || url.equals("/robots.txt"))
            {
                HttpResponse response = new HttpResponse();
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
