package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.extend.http.decode.ContentType;

import java.nio.charset.StandardCharsets;

public class Demo
{
    public static void main(String[] args) throws Exception
    {
        com.jfirer.jnet.extend.http.client.HttpClientImpl httpClient = new HttpClientImpl();
        String                                            url        = "http://localhost:2000/queryCommandList";
        int                                               count      = 0;
        while (true)
        {
            HttpSendRequest request = new HttpSendRequest();
            request.setUrl(url);
            request.setMethod("POST");
            request.setContentType(ContentType.APPLICATION_JSON);
            request.setBody("""
                            {
                                "hosId": "TestId",
                                "agentId": "1"
                            }
                            """);
            HttpReceiveResponse httpReceiveResponse = httpClient.newCall(request);
            IoBuffer            body                = httpReceiveResponse.getBody();
            System.out.println((count++) + ":" + StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString());
            httpReceiveResponse.close();
        }
    }
}
