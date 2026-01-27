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
        HttpConnection httpConnection = new HttpConnection("runanytime.hxi.me", 443, 60, true);
        HttpResponse   httpResponse   = httpConnection.write(new HttpRequest().setUrl("https://runanytime.hxi.me/").get(), 60);
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
        HttpConnection httpConnection = new HttpConnection("elysiver.h-e.top", 443, "127.0.0.1", 7879, true, 30, 30);

        // 创建请求并添加标准浏览器请求头（只支持 gzip，不支持 br）
        HttpRequest request = new HttpRequest()
                .setUrl("https://elysiver.h-e.top/")
                .get()
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36")
                .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
                .addHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .addHeader("Accept-Encoding", "gzip, deflate")
                .addHeader("Connection", "keep-alive");

        HttpResponse httpResponse = httpConnection.write(request, 60);

        // 打印响应头信息用于调试
        System.out.println("=== 响应头 ===");
        System.out.println("状态码: " + httpResponse.getHead().getStatusCode());
        System.out.println("响应头:");
        httpResponse.getHead().getHeaders().forEach((k, v) -> System.out.println("  " + k + ": " + v));
        System.out.println();

        // 打印响应体
        System.out.println("=== 响应体 ===");
        System.out.println(httpResponse.getBodyText());
        System.out.println("结束");
    }
}
