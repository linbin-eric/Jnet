package com.jfirer.jnet.extend.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class HttpRespHead implements HttpRespPart
{
    private int                 responseCode = 200;
    private Map<String, String> headers      = new HashMap<>();

    public HttpRespHead addHeader(String name, String value)
    {
        headers.put(name, value);
        return this;
    }

}
