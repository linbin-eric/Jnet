package cc.jfire.jnet.extend.http.dto;

import org.junit.Assert;
import org.junit.Test;

public class HttpUrlParseTest
{
    @Test
    public void parseDomainHttpUrl()
    {
        HttpUrl url = HttpUrl.parse("http://example.com:8080/api?q=1");

        Assert.assertEquals("example.com", url.domain());
        Assert.assertEquals(8080, url.port());
        Assert.assertEquals("/api?q=1", url.path());
        Assert.assertEquals("example.com:8080", url.hostHeader());
        Assert.assertFalse(url.ssl());
    }

    @Test
    public void parseDefaultHttpsPort()
    {
        HttpUrl url = HttpUrl.parse("https://example.com/");

        Assert.assertEquals("example.com", url.domain());
        Assert.assertEquals(443, url.port());
        Assert.assertEquals("/", url.path());
        Assert.assertEquals("example.com", url.hostHeader());
        Assert.assertTrue(url.ssl());
    }

    @Test
    public void parseBracketIpv6WithPort()
    {
        HttpUrl url = HttpUrl.parse("https://[2001:db8::1]:8443/index.html");

        Assert.assertEquals("2001:db8::1", url.domain());
        Assert.assertEquals(8443, url.port());
        Assert.assertEquals("/index.html", url.path());
        Assert.assertEquals("[2001:db8::1]:8443", url.hostHeader());
        Assert.assertTrue(url.ssl());
    }

    @Test
    public void parseBracketIpv6DefaultPort()
    {
        HttpUrl url = HttpUrl.parse("http://[::1]/");

        Assert.assertEquals("::1", url.domain());
        Assert.assertEquals(80, url.port());
        Assert.assertEquals("/", url.path());
        Assert.assertEquals("[::1]", url.hostHeader());
        Assert.assertFalse(url.ssl());
    }
}
