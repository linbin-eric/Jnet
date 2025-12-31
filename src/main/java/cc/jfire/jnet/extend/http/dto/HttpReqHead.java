package cc.jfire.jnet.extend.http.dto;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpReqHead implements HttpReq
{
    protected String              method;
    protected String              url;
    protected String              version;
    protected Map<String, String> headers       = new HashMap<>();
    protected int                 contentLength = 0;
    protected boolean             chunked       = false;

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }
}
