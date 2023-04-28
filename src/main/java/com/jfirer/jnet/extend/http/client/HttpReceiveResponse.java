package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class HttpReceiveResponse implements AutoCloseable
{
    private int                 httpCode;
    private Map<String, String> headers = new HashMap<>();
    private int                 contentLength;
    private String              contentType;
    private IoBuffer            body;

    public void putHeader(String name, String value)
    {
        headers.put(name, value);
    }

    @Override
    public void close() throws Exception
    {
        body.free();
    }
}
