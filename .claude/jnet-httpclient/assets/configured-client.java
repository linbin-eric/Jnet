import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 配置化 HTTP 客户端模板
 * 演示如何配置连接池、超时、代理等高级选项
 */
public class ConfiguredHttpClient {

    private final HttpClient client;

    public ConfiguredHttpClient() {
        // 创建自定义配置
        HttpClientConfig config = new HttpClientConfig()
            // 连接超时：15 秒
            .setConnectTimeoutSeconds(15)
            // 读取超时：120 秒
            .setReadTimeoutSeconds(120)
            // Keep-Alive 时间：1 小时
            .setKeepAliveSeconds(3600)
            // 每主机最大连接数：100
            .setMaxConnectionsPerHost(100)
            // 获取连接超时：5 秒
            .setAcquireTimeoutSeconds(5)
            // SSL 握手超时：30 秒
            .setSslHandshakeTimeoutSeconds(30);

        // 如果需要使用代理，取消下面两行注释
        // .setProxyHost("127.0.0.1")
        // .setProxyPort(7890);

        this.client = new HttpClient(config);
    }

    /**
     * 发起 GET 请求
     */
    public String get(String url) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(url)
                .get()
                .addHeader("User-Agent", "JNet-HttpClient/1.0")) {

            try (HttpResponse response = client.call(request)) {
                if (response.getHead().getStatusCode() != 200) {
                    throw new RuntimeException("请求失败，状态码: " + response.getHead().getStatusCode());
                }

                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    return StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                }
                return null;
            }
        }
    }

    /**
     * 发起 POST 请求
     */
    public String post(String url, String jsonBody, String authToken) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(url)
                .post()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + authToken)
                .setBody(jsonBody)) {

            try (HttpResponse response = client.call(request)) {
                int statusCode = response.getHead().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new RuntimeException("请求失败，状态码: " + statusCode);
                }

                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    return StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                }
                return null;
            }
        }
    }

    /**
     * 发起 PUT 请求
     */
    public String put(String url, String jsonBody, String authToken) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(url)
                .put()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + authToken)
                .setBody(jsonBody)) {

            try (HttpResponse response = client.call(request)) {
                int statusCode = response.getHead().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new RuntimeException("请求失败，状态码: " + statusCode);
                }

                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    return StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                }
                return null;
            }
        }
    }

    /**
     * 发起 DELETE 请求
     */
    public void delete(String url, String authToken) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(url)
                .delete()
                .addHeader("Authorization", "Bearer " + authToken)) {

            try (HttpResponse response = client.call(request)) {
                int statusCode = response.getHead().getStatusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    throw new RuntimeException("删除失败，状态码: " + statusCode);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        ConfiguredHttpClient client = new ConfiguredHttpClient();

        // 示例：GET 请求
        String result = client.get("https://api.example.com/users/1");
        System.out.println("GET 结果: " + result);

        // 示例：POST 请求
        String jsonBody = "{\"name\":\"John\",\"email\":\"john@example.com\"}";
        String postResult = client.post("https://api.example.com/users", jsonBody, "your-token");
        System.out.println("POST 结果: " + postResult);

        // 示例：PUT 请求
        String updateBody = "{\"name\":\"John Updated\"}";
        String putResult = client.put("https://api.example.com/users/1", updateBody, "your-token");
        System.out.println("PUT 结果: " + putResult);

        // 示例：DELETE 请求
        client.delete("https://api.example.com/users/1", "your-token");
        System.out.println("DELETE 完成");
    }
}
