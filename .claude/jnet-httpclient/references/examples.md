# JNet HttpClient 使用示例

## 基础示例

### 简单 GET 请求

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

public class SimpleGetExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/users")
                .get()) {

            try (HttpResponse response = client.call(request)) {
                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    String responseText = StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                    System.out.println("Response: " + responseText);
                }

                // 获取状态码
                int statusCode = response.getHead().getStatusCode();
                System.out.println("Status: " + statusCode);
            }
        }
    }
}
```

### 简单 POST 请求

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class SimplePostExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();

        String jsonBody = "{\"name\":\"John\",\"age\":30}";

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/users")
                .post()
                .setContentType("application/json")
                .setBody(jsonBody)) {

            try (HttpResponse response = client.call(request)) {
                // 处理响应
                System.out.println("Status: " + response.getHead().getStatusCode());
            }
        }
    }
}
```

## 配置示例

### 自定义超时和连接池

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;

public class ConfiguredClientExample {
    public static void main(String[] args) throws Exception {
        // 创建自定义配置
        HttpClientConfig config = new HttpClientConfig()
            .setConnectTimeoutSeconds(15)          // 连接超时 15 秒
            .setReadTimeoutSeconds(120)            // 读取超时 120 秒
            .setMaxConnectionsPerHost(100)         // 每主机最大 100 个连接
            .setKeepAliveSeconds(3600)             // Keep-Alive 1 小时
            .setAcquireTimeoutSeconds(5);          // 获取连接超时 5 秒

        HttpClient client = new HttpClient(config);

        // 使用配置好的客户端
        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/data")
                .get()) {
            try (HttpResponse response = client.call(request)) {
                // 处理响应
            }
        }
    }
}
```

### 使用代理

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class ProxyExample {
    public static void main(String[] args) throws Exception {
        // 配置代理
        HttpClientConfig config = new HttpClientConfig()
            .setProxyHost("127.0.0.1")
            .setProxyPort(7890);

        HttpClient client = new HttpClient(config);

        // 所有请求都会通过代理
        try (HttpRequest request = new HttpRequest()
                .setUrl("https://www.google.com")
                .get()) {
            try (HttpResponse response = client.call(request)) {
                System.out.println("Status: " + response.getHead().getStatusCode());
            }
        }
    }
}
```

## 高级示例

### 添加自定义请求头

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class CustomHeadersExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/protected")
                .get()
                .addHeader("Authorization", "Bearer your-token-here")
                .addHeader("X-Custom-Header", "custom-value")
                .addHeader("User-Agent", "JNet-HttpClient/1.0")) {

            try (HttpResponse response = client.call(request)) {
                // 处理响应
            }
        }
    }
}
```

### 流式响应处理

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.StreamableResponseFuture;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;
import cc.jfire.jnet.extend.http.dto.HttpResponseChunkedBodyPart;
import java.nio.charset.StandardCharsets;

public class StreamingExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();

        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/stream")
                .get()) {

            StreamableResponseFuture future = client.streamCall(
                request,
                // 处理每个响应片段
                part -> {
                    if (part instanceof HttpResponsePartHead) {
                        HttpResponsePartHead head = (HttpResponsePartHead) part;
                        System.out.println("Status: " + head.getStatusCode());
                    } else if (part instanceof HttpResponseChunkedBodyPart) {
                        HttpResponseChunkedBodyPart bodyPart = (HttpResponseChunkedBodyPart) part;
                        if (bodyPart.getChunk() != null) {
                            String chunk = StandardCharsets.UTF_8
                                .decode(bodyPart.getChunk().readableByteBuffer())
                                .toString();
                            System.out.print(chunk);
                        }
                    }

                    // 释放资源
                    part.free();

                    if (part.isLast()) {
                        System.out.println("\n流式响应完成");
                    }
                },
                // 错误处理
                error -> {
                    System.err.println("请求失败: " + error.getMessage());
                    error.printStackTrace();
                }
            );
        }
    }
}
```

### HTTPS 请求

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

public class HttpsExample {
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();

        // JNet 自动识别 HTTPS 并启用 SSL
        try (HttpRequest request = new HttpRequest()
                .setUrl("https://www.baidu.com")
                .get()) {

            try (HttpResponse response = client.call(request)) {
                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    String html = StandardCharsets.UTF_8
                        .decode(bodyBuffer.readableByteBuffer())
                        .toString();
                    System.out.println(html);
                }
            }
        }
    }
}
```

### 使用静态便捷方法

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class StaticMethodExample {
    public static void main(String[] args) throws Exception {
        // 使用默认实例的静态方法（向后兼容）
        try (HttpRequest request = new HttpRequest()
                .setUrl("https://api.example.com/data")
                .get()) {

            try (HttpResponse response = HttpClient.newCall(request)) {
                System.out.println("Status: " + response.getHead().getStatusCode());
            }
        }
    }
}
```

## 实际应用场景

### RESTful API 客户端

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

public class RestApiClient {
    private final HttpClient client;
    private final String baseUrl;
    private final String apiToken;

    public RestApiClient(String baseUrl, String apiToken) {
        this.baseUrl = baseUrl;
        this.apiToken = apiToken;

        HttpClientConfig config = new HttpClientConfig()
            .setConnectTimeoutSeconds(10)
            .setReadTimeoutSeconds(60)
            .setMaxConnectionsPerHost(50);

        this.client = new HttpClient(config);
    }

    public String getUser(int userId) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(baseUrl + "/users/" + userId)
                .get()
                .addHeader("Authorization", "Bearer " + apiToken)) {

            try (HttpResponse response = client.call(request)) {
                if (response.getHead().getStatusCode() != 200) {
                    throw new RuntimeException("请求失败: " + response.getHead().getStatusCode());
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

    public String createUser(String jsonBody) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(baseUrl + "/users")
                .post()
                .setContentType("application/json")
                .addHeader("Authorization", "Bearer " + apiToken)
                .setBody(jsonBody)) {

            try (HttpResponse response = client.call(request)) {
                if (response.getHead().getStatusCode() != 201) {
                    throw new RuntimeException("创建失败: " + response.getHead().getStatusCode());
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

    public void deleteUser(int userId) throws Exception {
        try (HttpRequest request = new HttpRequest()
                .setUrl(baseUrl + "/users/" + userId)
                .delete()
                .addHeader("Authorization", "Bearer " + apiToken)) {

            try (HttpResponse response = client.call(request)) {
                if (response.getHead().getStatusCode() != 204) {
                    throw new RuntimeException("删除失败: " + response.getHead().getStatusCode());
                }
            }
        }
    }
}
```

### 文件下载

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

public class FileDownloadExample {
    public static void downloadFile(String url, String outputPath) throws Exception {
        HttpClient client = new HttpClient();

        try (HttpRequest request = new HttpRequest().setUrl(url).get()) {
            try (HttpResponse response = client.call(request)) {
                if (response.getHead().getStatusCode() != 200) {
                    throw new RuntimeException("下载失败: " + response.getHead().getStatusCode());
                }

                IoBuffer bodyBuffer = response.getBodyBuffer();
                if (bodyBuffer != null) {
                    try (FileOutputStream fos = new FileOutputStream(outputPath);
                         FileChannel channel = fos.getChannel()) {
                        channel.write(bodyBuffer.readableByteBuffer());
                    }
                    System.out.println("文件已保存到: " + outputPath);
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        downloadFile("https://example.com/file.pdf", "/tmp/downloaded.pdf");
    }
}
```

### 批量请求

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BatchRequestExample {
    public static void main(String[] args) throws Exception {
        HttpClientConfig config = new HttpClientConfig()
            .setMaxConnectionsPerHost(100);  // 支持更多并发连接

        HttpClient client = new HttpClient(config);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        List<String> urls = List.of(
            "https://api.example.com/data/1",
            "https://api.example.com/data/2",
            "https://api.example.com/data/3"
        );

        List<CompletableFuture<Integer>> futures = new ArrayList<>();

        for (String url : urls) {
            CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
                try (HttpRequest request = new HttpRequest().setUrl(url).get()) {
                    try (HttpResponse response = client.call(request)) {
                        return response.getHead().getStatusCode();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return -1;
                }
            }, executor);

            futures.add(future);
        }

        // 等待所有请求完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // 输出结果
        for (int i = 0; i < urls.size(); i++) {
            System.out.println(urls.get(i) + " -> " + futures.get(i).get());
        }

        executor.shutdown();
    }
}
```
