package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.net.Socket;

public class Socks5HttpClientTest
{
    private static final String SOCKS5_PROXY_HOST = "127.0.0.1";
    private static final int    SOCKS5_PROXY_PORT = 10808;

    @Test
    public void httpOverRealSocks5Proxy() throws Exception
    {
        assumeRealSocks5ProxyAvailable();

        HttpClientConfig config = socks5Config();
        HttpResponse     response = new HttpClient(config).call(new HttpRequest().setUrl("http://example.com/").get());

        try (response)
        {
            Assert.assertEquals(200, response.getHead().getStatusCode());
            Assert.assertNotNull(response.getBodyText());
        }
    }

    @Test
    public void httpsOverRealSocks5Proxy() throws Exception
    {
        assumeRealSocks5ProxyAvailable();

        HttpClientConfig config = socks5Config();
        HttpResponse     response = new HttpClient(config).call(new HttpRequest().setUrl("https://example.com/").get());

        try (response)
        {
            Assert.assertEquals(200, response.getHead().getStatusCode());
            Assert.assertNotNull(response.getBodyText());
        }
    }

    private static HttpClientConfig socks5Config()
    {
        return new HttpClientConfig()
                .setProxyType(ProxyType.SOCKS5)
                .setProxyHost(SOCKS5_PROXY_HOST)
                .setProxyPort(SOCKS5_PROXY_PORT)
                .setReadTimeoutSeconds(15)
                .setAcquireTimeoutSeconds(3)
                .setSslHandshakeTimeoutSeconds(15);
    }

    private static void assumeRealSocks5ProxyAvailable()
    {
        Assume.assumeTrue("需要本机真实 SOCKS5 代理 socks5://127.0.0.1:10808", isPortOpen(SOCKS5_PROXY_HOST, SOCKS5_PROXY_PORT));
    }

    private static boolean isPortOpen(String host, int port)
    {
        try (Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(host, port), 1000);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
