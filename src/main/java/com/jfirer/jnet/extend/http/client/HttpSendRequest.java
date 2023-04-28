package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HttpSendRequest
{
    private String              url;
    private String              path;
    private String              doMain;
    private int                 port;
    private String              contentType;
    private Map<String, String> headers = new HashMap<>();
    private IoBuffer            body;
    private String              method;

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public void setBody(String body)
    {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        this.body = HttpClient.ALLOCATOR.ioBuffer(bytes.length);
        this.body.put(bytes);
    }
}
