package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class HttpRequestFixLengthBodyPart implements HttpRequestPart
{
    protected IoBuffer part;
}
