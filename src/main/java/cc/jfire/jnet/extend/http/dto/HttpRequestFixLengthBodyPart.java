package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class HttpRequestFixLengthBodyPart implements HttpRequestPart
{
    protected IoBuffer part;
    protected boolean  last = false;

    @Override
    public void close()
    {
        if (part != null)
        {
            part.free();
            part = null;
        }
    }
}
