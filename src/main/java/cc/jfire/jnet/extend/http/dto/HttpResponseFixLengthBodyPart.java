package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class HttpResponseFixLengthBodyPart implements HttpResponsePart
{
    protected IoBuffer part;

    @Override
    public void free()
    {
        if (part != null)
        {
            part.free();
            part = null;
        }
    }
}
