package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpResponse
{
    private Map<String, String> headers              = new HashMap<>();
    private String              body;
    private IoBuffer            bodyBuffer;
    private byte[]              bytes_body;
    private String              contentType;
    /**
     * 是否自动设置消息体长度，默认为 true
     */
    private boolean             autoSetContentLength = true;
    private boolean             autoSetContentType   = true;
}
