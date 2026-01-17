package cc.jfire.jnet;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.extend.http.client.HttpConnection;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import org.junit.Test;

import java.net.SocketTimeoutException;
import java.nio.channels.ClosedChannelException;
import java.nio.charset.StandardCharsets;

public class HttpsTest
{
    @Test
    public void test() throws ClosedChannelException, SocketTimeoutException
    {
        HttpConnection httpConnection = new HttpConnection("www.baidu.com", 443, 60, true);
        HttpResponse   httpResponse   = httpConnection.write(new HttpRequest().setUrl("https://www.baidu.com").get(), 60);
        IoBuffer       bodyBuffer     = httpResponse.getBodyBuffer();
        if (bodyBuffer != null)
        {
            String s = StandardCharsets.UTF_8.decode(bodyBuffer.readableByteBuffer()).toString();
            System.out.println(s);
        }
        System.out.println("结束");
    }

    @Test
    public void testproxy() throws ClosedChannelException, SocketTimeoutException
    {
        HttpConnection httpConnection = new HttpConnection("www.google.com",443,"127.0.0.1",7879,true,30,30);
        HttpResponse httpResponse = httpConnection.write(new HttpRequest().setUrl("https://www.google.com").get(),60);
        IoBuffer bodyBuffer = httpResponse.getBodyBuffer();
        if (bodyBuffer != null)
        {
            String s = StandardCharsets.UTF_8.decode(bodyBuffer.readableByteBuffer()).toString();
            System.out.println(s);
        }
        System.out.println("结束");
    }
}
