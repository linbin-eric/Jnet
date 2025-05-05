package com.jfirer.jnet.extend.http.dto;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.experimental.Accessors;

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
    /**
     * 是否自动设置消息体长度，默认为 true
     */
    private boolean  autoSetContentLength = false;
    private boolean  autoSetContentType   = false;

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
}
