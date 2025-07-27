package com.jfirer.jnet;

import com.jfirer.jnet.extend.http.client.HttpClient;
import com.jfirer.jnet.extend.http.client.HttpReceiveResponse;
import com.jfirer.jnet.extend.http.client.HttpSendRequest;
import com.jfirer.jnet.extend.http.client.PartOfBody;

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
        String          url     = "http://op.yynas.cn:2000/health";
        String          url2    = "http://127.0.0.1:10086/config/all";
        HttpSendRequest request = new HttpSendRequest().setUrl(url2).get();
        for (int i = 0; i < 50; i++)
        {
            try (HttpReceiveResponse receiveResponse = HttpClient.newCall(request))
            {
                String utf8Body = receiveResponse.getCachedUTF8Body();
                System.out.println(i + "   :   " + utf8Body + "，" + receiveResponse.getCachedUTF8Body());
            }
            catch (Throwable e)
            {
                e.printStackTrace();
            }
        }
    }

    private static void downloadFile() throws Exception
    {
        for (int i = 0; i < 10; i++)
        {
            HttpReceiveResponse receiveResponse = HttpClient.newCall(new HttpSendRequest().setUrl("http://yynas.cn:5678/1662645602015.jpg").get());
            File                file            = new File("/Users/linbin/Downloads/" + System.currentTimeMillis());
            file.createNewFile();
            FileChannel fileChannel = FileChannel.open(file.toPath(), StandardOpenOption.WRITE);
            PartOfBody  partOfBody;
            int         maxBytes    = 0;
            do
            {
                partOfBody = receiveResponse.pollChunk();
                if (partOfBody.isEndOrTerminateOfBody())
                {
                    break;
                }
                else
                {
                    int write = fileChannel.write(partOfBody.getEffectiveContent().readableByteBuffer());
                    if (write > maxBytes)
                    {
                        maxBytes = write;
                        System.out.println(maxBytes);
                    }
                    partOfBody.freeBuffer();
                }
            } while (true);
            receiveResponse.close();
            System.out.println("文件" + i);
        }
    }
}
