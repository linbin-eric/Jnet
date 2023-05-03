package com.jfirer.jnet;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.extend.http.client.HttpClient;
import com.jfirer.jnet.extend.http.client.HttpClientImpl;
import com.jfirer.jnet.extend.http.client.HttpReceiveResponse;
import com.jfirer.jnet.extend.http.client.HttpSendRequest;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;

public class HttpClientTest
{
    public static void main(String[] args) throws Exception
    {
        getInfo();
        System.exit(0);
    }

    private static void getInfo()
    {
        HttpClient      httpClient = new HttpClientImpl();
        String          url        = "http://localhost:10086/config/endpoint";
        String          url2       = "http://47.97.109.181:2000/health";
        HttpSendRequest request    = new HttpSendRequest().setUrl(url).getRequest();
        for (int i = 0; i < 50; i++)
        {
            try (HttpReceiveResponse receiveResponse = httpClient.newCall(request))
            {
                String utf8Body = receiveResponse.getUTF8Body();
                System.out.println(i + "   :   " + utf8Body);
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void downloadFile() throws Exception
    {
        HttpClient httpClient = new HttpClientImpl();
        for (int i = 0; i < 5; i++)
        {
            HttpReceiveResponse receiveResponse = httpClient.newCall(new HttpSendRequest().setUrl("http://yynas.cn:5678/operation2.sql").getRequest());
            File                file            = new File("/Users/linbin/Downloads/" + System.currentTimeMillis());
            file.createNewFile();
            FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            IoBuffer    buffer;
            int         maxBytes    = 0;
            do
            {
                buffer = receiveResponse.getChunked().take();
                if (receiveResponse.isEndOfChunked(buffer))
                {
                    break;
                }
                else
                {
                    int write = fileChannel.write(buffer.readableByteBuffer());
                    if (write > maxBytes)
                    {
                        maxBytes = write;
                        System.out.println(maxBytes);
                    }
                    buffer.free();
                }
            }
            while (true);
            receiveResponse.close();
            System.out.println("文件" + i);
        }
    }
}
