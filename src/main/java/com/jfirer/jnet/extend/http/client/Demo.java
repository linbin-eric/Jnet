package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class Demo
{
    public static void main(String[] args) throws Exception
    {
        com.jfirer.jnet.extend.http.client.HttpClientImpl httpClient = new HttpClientImpl();
//        String                                            url        = "http://localhost:2000/queryCommandList";
//        int                                               count      = 0;
//        while (true)
//        {
//            HttpSendRequest request = new HttpSendRequest();
//            request.setUrl(url);
//            request.setMethod("POST");
//            request.setContentType(ContentType.APPLICATION_JSON);
//            request.setBody("""
//                            {
//                                "hosId": "TestId",
//                                "agentId": "1"
//                            }
//                            """);
//            HttpReceiveResponse httpReceiveResponse = httpClient.newCall(request);
//            IoBuffer            body                = httpReceiveResponse.getBody();
//            System.out.println((count++) + ":" + StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString());
//            httpReceiveResponse.close();
//        }
        for (int i = 0; i < 50; i++)
        {
            HttpReceiveResponse receiveResponse = httpClient.newCall(new HttpSendRequest().setUrl("http://yynas.cn:5678/operation2.sql").getRequest());
            File                file            = new File("/Users/linbin/Downloads/" + System.currentTimeMillis());
            file.createNewFile();
            FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            IoBuffer    buffer;
            while ((buffer = receiveResponse.getStream().take()) != HttpReceiveResponse.END_OF_STREAM)
            {
                fileChannel.write(buffer.readableByteBuffer());
                buffer.free();
            }
            receiveResponse.close();
            System.out.println("文件" + i);
        }
    }
}
