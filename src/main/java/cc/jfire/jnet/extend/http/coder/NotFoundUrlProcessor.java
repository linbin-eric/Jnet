package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResponse;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class NotFoundUrlProcessor implements ReadProcessor<HttpRequest>
{
    private final NotFoundBarrier barrier;

    @Override
    public void read(HttpRequest data, ReadProcessorNode next)
    {
        String url      = data.getHead().getPath();
        String purePath = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
        barrier.notAvailablePaths.add(purePath);
        FullHttpResponse response = new FullHttpResponse();
        response.getHead().setStatusCode(404);
        response.getHead().setReasonPhrase("Not Found");
        response.setBodyText("notAvailable path:" + purePath);
        next.pipeline().fireWrite(response);
    }

    public static class NotFoundBarrier implements ReadProcessor<HttpRequest>
    {
        private Set<String> notAvailablePaths = new HashSet<>();

        @Override
        public void read(HttpRequest request, ReadProcessorNode next)
        {
            String url      = request.getHead().getPath();
            String purePath = url.contains("?") ? url.substring(0, url.indexOf("?")) : url;
            if (notAvailablePaths.contains(purePath))
            {
                FullHttpResponse response = new FullHttpResponse();
                response.getHead().setStatusCode(404);
                response.getHead().setReasonPhrase("Not Found");
                response.setBodyText("notAvailable path:" + purePath);
                next.pipeline().fireWrite(response);
            }
            else
            {
                next.fireRead(request);
            }
        }
    }
}
