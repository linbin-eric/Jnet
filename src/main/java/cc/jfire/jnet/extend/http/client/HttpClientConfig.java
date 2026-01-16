package cc.jfire.jnet.extend.http.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

@Data
@Accessors(chain = true)
public class HttpClientConfig
{
    /**
     * 连接超时时间（秒）
     */
    private             int            connectTimeoutSeconds      = 10;
    /**
     * 读取超时时间（秒）
     */
    private             int            readTimeoutSeconds         = 60;
    /**
     * Keep-Alive 时间（秒）
     */
    private             int            keepAliveSeconds           = 1800;
    /**
     * 每主机最大连接数
     */
    private             int            maxConnectionsPerHost      = 50;
    /**
     * 获取连接的超时时间（秒）
     */
    private             int            acquireTimeoutSeconds      = 1;
    /**
     * SSL 握手超时时间（秒）
     */
    private             int            sslHandshakeTimeoutSeconds = 30;
    public static final TrustManager[] TRUST_ANYONE               = new TrustManager[]{new X509TrustManager()
    {
        public X509Certificate[] getAcceptedIssuers()                            {return new X509Certificate[0];}

        public void checkClientTrusted(X509Certificate[] certs, String authType) {}

        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
    }};
}
