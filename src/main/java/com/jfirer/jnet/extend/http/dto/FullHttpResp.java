package com.jfirer.jnet.extend.http.dto;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class FullHttpResp implements HttpRespPart
{
    private             HttpRespHead head    = new HttpRespHead();
    private             HttpRespBody body    = new HttpRespBody();
    public static final byte[]       NEWLINE = "\r\n".getBytes(StandardCharsets.US_ASCII);

    private void helpSetContentLengthIfNeed()
    {
        if (head.hasContentLength() == false)
        {
            if (body.getBodyBuffer() != null && body.getBodyBuffer().remainRead() > 0)
            {
                head.addHeader("Content-Length", String.valueOf(body.getBodyBuffer().remainRead()));
            }
            else if (body.getBodyBytes() != null)
            {
                head.addHeader("Content-Length", String.valueOf(body.getBodyBytes().length));
            }
            else if (body.getBodyText() != null)
            {
                byte[] array = body.getBodyText().getBytes(StandardCharsets.UTF_8);
                head.addHeader("Content-Length", String.valueOf(array.length));
            }
            else
            {
                head.addHeader("Content-Length", "0");
            }
        }
    }

    private void helpSetContentTypeIfNeed()
    {
        if (head.hasContentType() == false)
        {
            head.addHeader("content-type", "application/json;charset=utf8");
        }
    }

    public void write(IoBuffer buffer)
    {
        helpSetContentLengthIfNeed();
        helpSetContentTypeIfNeed();
        head.write(buffer);
        buffer.put(NEWLINE);
        body.write(buffer);
    }
}
