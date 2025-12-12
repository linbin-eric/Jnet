package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.WriteProcessor;
import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.extend.http.dto.FullHttpResp;
import cc.jfire.jnet.extend.http.dto.HttpRespHead;

public class CorsEncoder implements WriteProcessor<Object>
{
    @Override
    public void write(Object data, WriteProcessorNode next)
    {
        if (data instanceof FullHttpResp fullHttpResp)
        {
            HttpRespHead head = fullHttpResp.getHead();
            head.addHeader("Access-Control-Allow-Origin", "*")//
                .addHeader("access-control-allow-methods", "GET,PUT,POST,HEAD")//
                .addHeader("Access-Control-Max-Age", "86400")//
                .addHeader("Access-Control-Allow-Headers", "*");
            next.fireWrite(fullHttpResp);
        }
        else
        {
            next.fireWrite(data);
        }
    }
}
