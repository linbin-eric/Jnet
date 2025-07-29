package com.jfirer.jnet.extend.http.dto;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.experimental.Accessors;

import java.nio.charset.StandardCharsets;

@Data
@Accessors(chain = true)
public class HttpRespBody implements HttpRespPart
{
    /**
     * 下面的三种会存在任意一种，优先级是bodyBuffer > bytes_body > bodyText
     */
    private IoBuffer bodyBuffer;
    private byte[]   bodyBytes;
    private String   bodyText;

    public boolean isEmpty()
    {
        if ((bodyText == null && bodyBuffer == null && bodyBytes == null)//
            || (bodyBuffer != null && bodyBuffer.remainRead() == 0)//
            || (bodyBytes != null && bodyBytes.length == 0))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 将Body的内容写入到buffer。
     * 注意：如果body的存储是IoBuffer，write的时候会将它free
     * @param buffer
     */
    public void write(IoBuffer buffer)
    {
        if (bodyBuffer != null)
        {
            buffer.put(bodyBuffer);
            bodyBuffer.free();
            bodyBuffer = null;
        }
        else if (bodyBytes != null)
        {
            buffer.put(bodyBytes);
        }
        else if (bodyText != null)
        {
            buffer.put(bodyText.getBytes(StandardCharsets.UTF_8));
        }
    }
}
