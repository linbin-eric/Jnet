import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 基础 HTTP 客户端模板
 * 演示如何使用 JNet HttpClient 发起简单的 GET 和 POST 请求
 */
public class BasicHttpClient {

    public static void main(String[] args) throws Exception {
        // 创建 HttpClient 实例
        HttpClient client = new HttpClient();

        // 示例 1: GET 请求
        getExample(client);

        // 示例 2: POST 请求
        postExample(client);
    }

    /**
     * GET 请求示例
     */
    private static void getExample(HttpClient client) throws Exception {
        System.out.println("=== GET 请求示例 ===");

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/users/1")
                .get()
                .addHeader("User-Agent", "JNet-HttpClient/1.0")) {

            try (HttpResponse response = client.call(request)) {
                // 获取状态码
                int statusCode = response.getHead().getStatusCode();
                System.out.println("状态码: " + statusCode);

                // 读取响应体
                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    String responseText = StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                    System.out.println("响应内容: " + responseText);
                }
            }
        }
    }

    /**
     * POST 请求示例
     */
    private static void postExample(HttpClient client) throws Exception {
        System.out.println("\n=== POST 请求示例 ===");

        String jsonBody = "{\"name\":\"John Doe\",\"email\":\"john@example.com\"}";

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/users")
                .post()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer your-token-here")
                .setBody(jsonBody)) {

            try (HttpResponse response = client.call(request)) {
                int statusCode = response.getHead().getStatusCode();
                System.out.println("状态码: " + statusCode);

                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    String responseText = StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                    System.out.println("响应内容: " + responseText);
                }
            }
        }
    }
}
