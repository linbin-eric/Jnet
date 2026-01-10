package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class OptionsProcessor implements ReadProcessor<Object>
{
    @Override
    public void read(Object obj, ReadProcessorNode next)
    {
        if (obj instanceof HttpRequest request)
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
        else
        {
            next.fireRead(obj);
        }
    }
}
