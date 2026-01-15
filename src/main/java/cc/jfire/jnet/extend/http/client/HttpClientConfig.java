package cc.jfire.jnet.extend.http.client;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.net.ssl.TrustManager;

@Data
@Accessors(chain = true)
public class HttpClientConfig
{
    /** 连接超时时间（秒） */
    private int connectTimeoutSeconds = 10;

    /** 读取超时时间（秒） */
    private int readTimeoutSeconds = 60;

    /** Keep-Alive 时间（秒） */
    private int keepAliveSeconds = 1800;

    /** 每主机最大连接数 */
    private int maxConnectionsPerHost = 50;

    /** 获取连接的超时时间（秒） */
    private int acquireTimeoutSeconds = 1;

    /** 自定义 TrustManager（可选，null 表示信任所有证书） */
    private TrustManager[] trustManagers;

    /** SSL 握手超时时间（秒） */
    private int sslHandshakeTimeoutSeconds = 30;
}
