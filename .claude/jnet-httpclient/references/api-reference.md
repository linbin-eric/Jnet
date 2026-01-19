# JNet HttpClient API 参考

## 核心类

### HttpClient

HTTP 客户端主类，管理连接池并发起 HTTP 请求。

**构造方法：**
```java
// 使用默认配置
HttpClient client = new HttpClient();

// 使用自定义配置
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(10)
    .setReadTimeoutSeconds(60);
HttpClient client = new HttpClient(config);
```

**主要方法：**

- `HttpResponse call(HttpRequest request)` - 发起同步 HTTP 请求，返回完整响应
- `StreamableResponseFuture streamCall(HttpRequest request, Consumer<HttpResponsePart> partConsumer, Consumer<Throwable> errorConsumer)` - 发起流式 HTTP 请求，通过回调处理响应片段

**静态便捷方法（向后兼容）：**

- `HttpClient.newCall(HttpRequest)` - 使用默认实例发起同步请求
- `HttpClient.newStreamCall(HttpRequest, partConsumer, errorConsumer)` - 使用默认实例发起流式请求
- `HttpClient.getDefault()` - 获取默认实例

### HttpClientConfig

HTTP 客户端配置类，支持链式调用。

**配置项：**

| 字段 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| connectTimeoutSeconds | int | 10 | 连接超时时间（秒） |
| readTimeoutSeconds | int | 60 | 读取超时时间（秒） |
| keepAliveSeconds | int | 1800 | Keep-Alive 时间（秒） |
| maxConnectionsPerHost | int | 50 | 每主机最大连接数 |
| acquireTimeoutSeconds | int | 1 | 获取连接的超时时间（秒） |
| sslHandshakeTimeoutSeconds | int | 30 | SSL 握手超时时间（秒） |
| proxyHost | String | null | 代理服务器主机名 |
| proxyPort | int | 0 | 代理服务器端口 |

**方法：**

- `boolean hasProxy()` - 判断是否配置了代理

**示例：**
```java
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(15)
    .setReadTimeoutSeconds(120)
    .setMaxConnectionsPerHost(100)
    .setProxyHost("127.0.0.1")
    .setProxyPort(7890);
```

### HttpRequest

HTTP 请求对象，支持链式调用。

**主要方法：**

- `setUrl(String url)` - 设置请求 URL（自动识别 HTTP/HTTPS）
- `setMethod(String method)` - 设置请求方法
- `get()` / `post()` / `put()` / `delete()` - 快捷设置请求方法
- `addHeader(String name, String value)` - 添加请求头
- `setContentType(String contentType)` - 设置 Content-Type
- `setBody(String body)` - 设置字符串请求体
- `setBody(IoBuffer body)` - 设置 IoBuffer 请求体
- `close()` - 释放资源（必须调用）

**示例：**
```java
HttpRequest request = new HttpRequest()
    .setUrl("https://api.example.com/users")
    .post()
    .setContentType("application/json")
    .addHeader("Authorization", "Bearer token")
    .setBody("{\"name\":\"John\"}");
```

### HttpResponse

HTTP 响应对象。

**主要方法：**

- `HttpResponsePartHead getHead()` - 获取响应头部（包含状态码、响应头等）
- `IoBuffer getBodyBuffer()` - 获取响应体 IoBuffer
- `byte[] getBodyBytes()` - 获取响应体字节数组
- `close()` - 释放资源（必须调用）

**读取响应体：**
```java
// 方式 1：从 IoBuffer 读取
IoBuffer bodyBuffer = response.getBodyBuffer();
if (bodyBuffer != null) {
    String text = StandardCharsets.UTF_8.decode(bodyBuffer.readableByteBuffer()).toString();
}

// 方式 2：从字节数组读取
byte[] bodyBytes = response.getBodyBytes();
if (bodyBytes != null) {
    String text = new String(bodyBytes, StandardCharsets.UTF_8);
}
```

### HttpResponsePart

流式响应片段接口。

**方法：**

- `void free()` - 释放资源
- `boolean isLast()` - 是否是最后一个片段

**子类型：**

- `HttpResponsePartHead` - 响应头部片段
- `HttpResponseFixLengthBodyPart` - 固定长度响应体片段
- `HttpResponseChunkedBodyPart` - 分块响应体片段

## 资源管理

**重要：** JNet 使用池化缓冲区，必须手动释放资源：

```java
HttpRequest request = new HttpRequest().setUrl("https://example.com").get();
try {
    HttpResponse response = client.call(request);
    try {
        // 处理响应
    } finally {
        response.close(); // 释放响应资源
    }
} catch (Exception e) {
    request.close(); // 异常时释放请求资源
}
```

**推荐使用 try-with-resources：**

```java
try (HttpRequest request = new HttpRequest().setUrl("https://example.com").get()) {
    try (HttpResponse response = client.call(request)) {
        // 处理响应
    }
}
```

## SSL/TLS 支持

JNet 自动识别 HTTPS URL 并启用 SSL：

```java
// 自动启用 SSL
HttpRequest request = new HttpRequest()
    .setUrl("https://secure.example.com/api")
    .get();
```

**信任所有证书（仅用于测试）：**

```java
HttpClientConfig config = new HttpClientConfig();
// 注意：TRUST_ANYONE 会跳过证书验证，生产环境不推荐使用
```

## 代理支持

通过 `HttpClientConfig` 配置代理：

```java
HttpClientConfig config = new HttpClientConfig()
    .setProxyHost("127.0.0.1")
    .setProxyPort(7890);

HttpClient client = new HttpClient(config);

// 所有请求都会通过代理
HttpResponse response = client.call(
    new HttpRequest().setUrl("https://www.google.com").get()
);
```

## 连接池

JNet 内置连接池，自动管理连接复用：

- 每个 `HttpClient` 实例维护独立的连接池
- 连接按 `(host, port, ssl)` 分组
- 支持 Keep-Alive 连接复用
- 自动处理连接借用和归还

**配置连接池：**

```java
HttpClientConfig config = new HttpClientConfig()
    .setMaxConnectionsPerHost(100)      // 每主机最大连接数
    .setKeepAliveSeconds(1800)          // Keep-Alive 时间
    .setAcquireTimeoutSeconds(5);       // 获取连接超时

HttpClient client = new HttpClient(config);
```

## 超时控制

JNet 支持多种超时配置：

```java
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(10)           // 连接超时
    .setReadTimeoutSeconds(60)              // 读取超时
    .setSslHandshakeTimeoutSeconds(30)      // SSL 握手超时
    .setAcquireTimeoutSeconds(1);           // 获取连接超时
```
