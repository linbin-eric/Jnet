# JNet 使用手册

JNet 是一个基于 Java AIO（异步 I/O）构建的高性能网络通信框架，支持 TCP 客户端/服务器、HTTP/HTTPS、WebSocket 和反向代理功能。

## 目录

1. [快速入门](#快速入门)
2. [核心概念](#核心概念)
3. [TCP 服务器与客户端](#tcp-服务器与客户端)
4. [HTTP 服务器](#http-服务器)
5. [HTTP 客户端](#http-客户端)
6. [WebSocket](#websocket)
7. [缓冲区系统](#缓冲区系统)
8. [编解码器](#编解码器)
9. [反向代理](#反向代理)
10. [最佳实践](#最佳实践)

---

## 快速入门

### 环境要求

- **Java 版本**: 21+（使用虚拟线程）
- **构建工具**: Maven 3+

### Maven 依赖

```xml
<dependency>
    <groupId>cc.jfire</groupId>
    <artifactId>jnet</artifactId>
    <version>1.0.1-SNAPSHOT</version>
</dependency>
```

### 最简单的 TCP Echo 服务器

```java
import cc.jfire.jnet.server.AioServer;
import cc.jfire.jnet.common.api.ChannelConfig;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

public class EchoServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            pipeline.addReadProcessor((ReadProcessor<IoBuffer>) (data, next) -> {
                // Echo: 将收到的数据原样返回
                next.pipeline().fireWrite(data);
            });
        });

        server.start();
        System.out.println("Echo 服务器启动在端口 8080");
    }
}
```

---

## 核心概念

### Pipeline 模式（管道模式）

JNet 的核心设计基于**处理器链模式**，数据通过串联的处理器流动：

```
入站流程（读）：
IoBuffer(网络数据) → [ReadProcessor1] → [ReadProcessor2] → ... → 业务逻辑

出站流程（写）：
业务数据 → [WriteProcessor1] → [WriteProcessor2] → ... → IoBuffer → 网络发送
```

### 核心接口

| 接口 | 说明 |
|------|------|
| `Pipeline` | 管道接口，管理处理器链，提供数据写出方法 |
| `ReadProcessor<T>` | 读处理器，处理入站数据 |
| `WriteProcessor<T>` | 写处理器，处理出站数据 |
| `ReadProcessorNode` | 读处理器节点，用于传递数据到下一个处理器 |
| `WriteProcessorNode` | 写处理器节点，用于传递数据到下一个处理器 |
| `BufferAllocator` | 缓冲区分配器，分配和管理 IoBuffer |

### Pipeline 关键方法

```java
public interface Pipeline {
    // 写出数据（通过写处理器链）
    void fireWrite(Object data);

    // 直接写入（跳过处理器链）
    void directWrite(IoBuffer buffer);

    // 添加读/写处理器
    void addReadProcessor(ReadProcessor<?> processor);
    void addWriteProcessor(WriteProcessor<?> processor);

    // 关闭输入
    void shutdownInput();

    // 获取分配器
    BufferAllocator allocator();

    // 持久化存储（处理器间共享数据）
    void putPersistenceStore(String key, Object value);
    Object getPersistenceStore(String key);
}
```

---

## TCP 服务器与客户端

### TCP 服务器

#### 基本用法

```java
import cc.jfire.jnet.server.AioServer;
import cc.jfire.jnet.common.api.ChannelConfig;
import cc.jfire.jnet.common.coder.TotalLengthFieldBasedFrameDecoder;
import cc.jfire.jnet.common.processor.LengthEncoder;

ChannelConfig config = new ChannelConfig();
config.setPort(8080);
config.setIp("0.0.0.0");  // 监听所有接口

AioServer server = AioServer.newAioServer(config, pipeline -> {
    // 添加读处理器链
    pipeline.addReadProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024*1024));
    pipeline.addReadProcessor((ReadProcessor<IoBuffer>) (data, next) -> {
        // 业务逻辑处理
        System.out.println("收到数据：" + data.remainRead() + " 字节");

        // 构建响应
        IoBuffer response = next.pipeline().allocator().allocate(512);
        response.putInt(0); // 预留长度位置
        response.put("Hello".getBytes());
        next.pipeline().fireWrite(response);
    });

    // 添加写处理器链
    pipeline.addWriteProcessor(new LengthEncoder(0, 4));
});

server.start();

// 关闭服务器
server.shutdown();      // 停止接收新连接
server.termination();   // 终止所有连接
```

#### ChannelConfig 配置选项

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `port` | -1 | 监听端口 |
| `ip` | "0.0.0.0" | 监听地址 |
| `backLog` | 50 | 积压队列大小 |
| `maxReceiveSize` | 8MB | 最大接收大小 |
| `initReceiveSize` | 1KB | 初始接收大小 |
| `maxBatchWrite` | 2MB | 最大批量写大小 |

### TCP 客户端

```java
import cc.jfire.jnet.client.ClientChannel;

ChannelConfig config = new ChannelConfig();
config.setIp("127.0.0.1");
config.setPort(8080);

ClientChannel client = ClientChannel.newClient(config, pipeline -> {
    pipeline.addReadProcessor(new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024*1024));
    pipeline.addReadProcessor((ReadProcessor<IoBuffer>) (data, next) -> {
        System.out.println("收到响应：" + data.remainRead() + " 字节");
    });
    pipeline.addWriteProcessor(new LengthEncoder(0, 4));
});

// 连接服务器
if (client.connect()) {
    // 发送数据
    IoBuffer buffer = client.pipeline().allocator().allocate(512);
    buffer.putInt(0); // 预留长度
    buffer.put("Hello Server".getBytes());
    client.pipeline().fireWrite(buffer);
} else {
    System.err.println("连接失败：" + client.getConnectionException());
}

// 检查连接状态
if (client.alive()) {
    System.out.println("连接仍有效");
}
```

---

## HTTP 服务器

### 基本 HTTP 服务器

```java
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.*;

ChannelConfig config = new ChannelConfig().setPort(8080);

AioServer server = AioServer.newAioServer(config, pipeline -> {
    // HTTP 解码处理器链
    pipeline.addReadProcessor(new HttpRequestPartDecoder());
    pipeline.addReadProcessor(new HttpRequestAggregator());

    // 业务处理器
    pipeline.addReadProcessor((ReadProcessor<HttpRequest>) (request, next) -> {
        try {
            System.out.println(request.getHead().getMethod() + " " + request.getHead().getPath());

            HttpResponse response = new HttpResponse();
            response.addHeader("Content-Type", "text/html; charset=UTF-8");
            response.setBodyText("<h1>Hello JNet!</h1>");

            next.pipeline().fireWrite(response);
        } finally {
            request.close(); // 释放资源
        }
    });

    // HTTP 编码处理器
    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
});

server.start();
```

### HTTPS 服务器

```java
import javax.net.ssl.*;
import cc.jfire.jnet.extend.http.coder.ssl.*;

AioServer server = AioServer.newAioServer(config, pipeline -> {
    // 配置 SSL
    KeyStore keyStore = KeyStore.getInstance("JKS");
    try (InputStream fis = getClass().getResourceAsStream("/keystore.jks")) {
        keyStore.load(fis, "password".toCharArray());
    }

    KeyManagerFactory kmf = KeyManagerFactory.getInstance(
        KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(keyStore, "password".toCharArray());

    TrustManagerFactory tmf = TrustManagerFactory.getInstance(
        TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(keyStore);

    SSLContext sslContext = SSLContext.getInstance("TLS");
    sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

    SSLEngine sslEngine = sslContext.createSSLEngine();
    sslEngine.setUseClientMode(false);
    sslEngine.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});

    SSLDecoder sslDecoder = new SSLDecoder(sslEngine);
    SSLEncoder sslEncoder = new SSLEncoder(sslEngine);
    sslEngine.beginHandshake();

    // SSL 必须在最前面
    pipeline.addReadProcessor(sslDecoder);
    pipeline.addReadProcessor(new HttpRequestPartDecoder());
    pipeline.addReadProcessor(new HttpRequestAggregator());
    pipeline.addReadProcessor(new OptionsProcessor()); // CORS 支持
    pipeline.addReadProcessor((ReadProcessor<HttpRequest>) (request, next) -> {
        // 处理请求...
    });

    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
    pipeline.addWriteProcessor(sslEncoder);
});
```

### HttpRequest 和 HttpResponse

```java
// 请求对象
HttpRequest request;
String method = request.getHead().getMethod();    // GET, POST, PUT, DELETE
String path = request.getHead().getPath();        // /api/users
String version = request.getHead().getVersion();  // HTTP/1.1
String contentType = request.getHead().getHeaders().get("Content-Type");
IoBuffer body = request.getBody();                // 请求体
String strBody = request.getStrBody();            // 字符串形式的请求体

// 响应对象
HttpResponse response = new HttpResponse();
response.getHead().setStatusCode(200);
response.getHead().setStatusMessage("OK");
response.addHeader("Content-Type", "application/json");
response.addHeader("Cache-Control", "no-cache");
response.setBodyText("{\"status\":\"success\"}");
```

---

## HTTP 客户端

### 同步请求

```java
import cc.jfire.jnet.extend.http.client.*;

HttpClient client = new HttpClient();

// GET 请求
HttpResponse response = client.call(
    new HttpRequest()
        .setUrl("https://api.example.com/users")
        .get()
        .addHeader("Authorization", "Bearer token123")
);

// POST 请求
HttpResponse response = client.call(
    new HttpRequest()
        .setUrl("https://api.example.com/users")
        .post()
        .addHeader("Content-Type", "application/json")
        .setBody("{\"name\":\"John\",\"email\":\"john@example.com\"}")
);

// 处理响应
int statusCode = response.getHead().getStatusCode();
if (statusCode == 200) {
    IoBuffer body = response.getBodyBuffer();
    String content = StandardCharsets.UTF_8.decode(body.readableByteBuffer()).toString();
    System.out.println(content);
}
response.close(); // 释放资源
```

### 流式请求

```java
StreamableResponseFuture future = client.streamCall(
    new HttpRequest().setUrl("http://example.com/large-file").get(),
    // 处理响应块
    part -> {
        if (part instanceof HttpResponsePartHead head) {
            System.out.println("状态码：" + head.getStatusCode());
        } else if (part instanceof HttpResponseFixLengthBodyPart body) {
            // 处理响应体块
            IoBuffer data = body.getPart();
            // 处理数据...
            data.free();
        }
    },
    // 错误处理
    error -> System.err.println("请求失败：" + error)
);
```

### 客户端配置

```java
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(10)           // 连接超时（默认10秒）
    .setReadTimeoutSeconds(60)              // 读超时（默认60秒）
    .setKeepAliveSeconds(1800)              // Keep-Alive 时间（默认1800秒）
    .setMaxConnectionsPerHost(50)           // 单主机最大连接数（默认50）
    .setSslHandshakeTimeoutSeconds(30)      // SSL 握手超时（默认30秒）
    .setProxyHost("proxy.example.com")      // 代理主机
    .setProxyPort(8080);                    // 代理端口

HttpClient client = new HttpClient(config);
```

---

## WebSocket

### WebSocket 服务器

```java
import cc.jfire.jnet.extend.websocket.coder.*;
import cc.jfire.jnet.extend.websocket.dto.*;

AioServer server = AioServer.newAioServer(config, pipeline -> {
    // WebSocket 升级处理器（处理 HTTP 升级请求）
    pipeline.addReadProcessor(new WebSocketUpgradeDecoder());

    // WebSocket 帧解码器（true = 服务端模式，要求客户端帧 MASK=1）
    pipeline.addReadProcessor(new WebSocketFrameDecoder(true));

    // 业务处理器
    pipeline.addReadProcessor((ReadProcessor<WebSocketFrame>) (frame, next) -> {
        try {
            switch (frame.getOpcode()) {
                case WebSocketFrame.OPCODE_TEXT -> {
                    String text = StandardCharsets.UTF_8.decode(
                        frame.getPayload().readableByteBuffer()).toString();
                    System.out.println("收到文本：" + text);

                    // 回复
                    WebSocketFrame response = new WebSocketFrame();
                    response.setOpcode(WebSocketFrame.OPCODE_TEXT);
                    IoBuffer payload = next.pipeline().allocator().allocate(256);
                    payload.put(("Echo: " + text).getBytes(StandardCharsets.UTF_8));
                    response.setPayload(payload);
                    next.pipeline().fireWrite(response);
                }
                case WebSocketFrame.OPCODE_BINARY -> {
                    // 处理二进制数据
                }
                case WebSocketFrame.OPCODE_PING -> {
                    next.pipeline().fireWrite(WebSocketFrame.createPong(frame.getPayload()));
                }
                case WebSocketFrame.OPCODE_CLOSE -> {
                    next.pipeline().shutdownInput();
                }
            }
        } finally {
            frame.free();
        }
    });

    // WebSocket 帧编码器
    pipeline.addWriteProcessor(new WebSocketFrameEncoder(true));
});
```

### WebSocketFrame 操作码

| 操作码 | 常量 | 说明 |
|--------|------|------|
| 0 | `OPCODE_CONTINUATION` | 延续帧 |
| 1 | `OPCODE_TEXT` | 文本帧 |
| 2 | `OPCODE_BINARY` | 二进制帧 |
| 8 | `OPCODE_CLOSE` | 关闭帧 |
| 9 | `OPCODE_PING` | Ping 帧 |
| 10 | `OPCODE_PONG` | Pong 帧 |

### 创建 WebSocketFrame

```java
// 文本帧
WebSocketFrame textFrame = new WebSocketFrame();
textFrame.setOpcode(WebSocketFrame.OPCODE_TEXT);
textFrame.setFin(true);  // 是否是最后一帧
IoBuffer payload = allocator.allocate(256);
payload.put("Hello".getBytes(StandardCharsets.UTF_8));
textFrame.setPayload(payload);

// Pong 响应
WebSocketFrame pong = WebSocketFrame.createPong(pingPayload);

// 关闭帧
WebSocketFrame close = WebSocketFrame.createClose(1000, "正常关闭");
```

---

## 缓冲区系统

### IoBuffer 核心方法

```java
BufferAllocator allocator = pipeline.allocator();

// 分配缓冲区
IoBuffer buffer = allocator.allocate(1024);

// 写入数据
buffer.put((byte) 0x01);
buffer.put("Hello".getBytes());
buffer.putInt(100);
buffer.putShort((short) 50);
buffer.put(otherBuffer);  // 写入另一个缓冲区

// 读取数据
byte b = buffer.get();
int i = buffer.getInt();
short s = buffer.getShort();

// 位置操作
int readable = buffer.remainRead();   // 可读字节数
int readPos = buffer.getReadPosi();   // 读位置
buffer.setReadPosi(0);                // 设置读位置

// 切片和压缩
IoBuffer slice = buffer.slice(100);   // 切出100字节
buffer.compact();                      // 压缩已读数据

// 转换为 ByteBuffer
ByteBuffer byteBuffer = buffer.readableByteBuffer();

// 释放资源（重要！）
buffer.free();
```

### 分配器类型

```java
// 池化分配器（推荐，生产环境使用）
BufferAllocator pooled = new PooledBufferAllocator(5000, true, arena);

// 非池化分配器（开发/调试使用）
BufferAllocator unpooled = new UnPoolBufferAllocator(true); // true = 堆外内存
```

---

## 编解码器

### 内置编解码器

| 编解码器 | 说明 |
|----------|------|
| `TotalLengthFieldBasedFrameDecoder` | 固定长度字段帧解码器 |
| `LengthEncoder` | 长度字段编码器 |
| `HttpRequestPartDecoder` | HTTP 请求解码器 |
| `HttpRequestAggregator` | HTTP 请求聚合器 |
| `HttpRespEncoder` | HTTP 响应编码器 |
| `SSLDecoder` / `SSLEncoder` | SSL/TLS 编解码器 |
| `WebSocketFrameDecoder` / `WebSocketFrameEncoder` | WebSocket 帧编解码器 |

### TotalLengthFieldBasedFrameDecoder 参数

```java
new TotalLengthFieldBasedFrameDecoder(
    lengthFieldOffset,   // 长度字段起始位置（字节）
    lengthFieldLength,   // 长度字段长度（1/2/4字节）
    skipBytes,           // 解析后跳过的字节数
    maxLength            // 最大报文长度
);

// 示例：长度在前4字节，跳过4字节长度头
new TotalLengthFieldBasedFrameDecoder(0, 4, 4, 1024*1024);
```

### 自定义解码器

```java
public class MyDecoder extends AbstractDecoder {
    private int state = 0;

    @Override
    protected void process0(ReadProcessorNode next) {
        while (true) {
            switch (state) {
                case 0 -> {
                    if (accumulation.remainRead() < 4) return; // 数据不足，等待
                    int length = accumulation.getInt();
                    state = 1;
                }
                case 1 -> {
                    if (accumulation.remainRead() < length) return;
                    IoBuffer data = accumulation.slice(length);
                    next.fireRead(data); // 传递给下一个处理器
                    state = 0;
                }
            }
        }
    }
}
```

### 自定义编码器

```java
public class MyEncoder implements WriteProcessor<MyData> {
    @Override
    public void write(MyData data, WriteProcessorNode next) {
        IoBuffer buffer = next.pipeline().allocator().allocate(256);
        buffer.putInt(data.getLength());
        buffer.put(data.getContent());
        next.fireWrite(buffer);
    }
}
```

---

## 反向代理

### 配置文件方式

创建 `reverse.config` 文件：

```yaml
9001:                          # 监听端口
  /:                           # 路径
    type: resource             # 类型：resource（文件）或 proxy（代理）
    path: file:/var/www/html   # 文件路径
    order: 2                   # 优先级（数字越小优先级越高）
  /api/*:
    type: proxy
    path: http://api-server:8080/
    order: 1
  ssl:
    enable: true
    cert: /path/to/cert.pem
    password: cert_password
```

### 程序化配置

```java
import cc.jfire.reverse.proxy.ReverseProxyServer;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;

List<ResourceConfig> configs = new ArrayList<>();

// 添加静态资源
configs.add(ResourceConfig.io("/static", "file:/var/www/static", 2));

// 添加代理规则（完全匹配）
configs.add(ResourceConfig.fullMatch("/api/v1/users", "http://api-server:8080/", 1));

// 添加代理规则（前缀匹配）
configs.add(ResourceConfig.prefixMatch("/api/*", "http://api-server:8080/", 1));

ReverseProxyServer proxyServer = new ReverseProxyServer(9001, configs);
proxyServer.start();
```

---

## 最佳实践

### 1. 资源管理

```java
// IoBuffer 使用完毕后必须释放
IoBuffer buffer = allocator.allocate(1024);
try {
    // 使用 buffer...
} finally {
    buffer.free();
}

// HttpRequest/HttpResponse 使用 try-finally 或 try-with-resources
try (HttpRequest request = new HttpRequest()) {
    // 使用 request...
}
```

### 2. 处理器链顺序

典型的 HTTPS 服务器处理器链：

```
读处理器链：
1. SSLDecoder（SSL 解密）
2. HttpRequestPartDecoder（HTTP 解码）
3. HttpRequestAggregator（请求聚合）
4. OptionsProcessor（CORS 处理，可选）
5. 业务处理器

写处理器链：
1. HttpRespEncoder（HTTP 编码）
2. SSLEncoder（SSL 加密）
```

### 3. 错误处理

```java
pipeline.addReadProcessor(new ReadProcessor<IoBuffer>() {
    @Override
    public void read(IoBuffer data, ReadProcessorNode next) {
        try {
            // 处理数据
        } catch (Exception e) {
            next.fireReadFailed(e);
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next) {
        log.error("处理失败", e);
        next.pipeline().shutdownInput();
    }
});
```

### 4. 连接状态检查

```java
if (client.alive()) {
    // 连接有效，可以发送数据
    client.pipeline().fireWrite(data);
} else {
    // 需要重连
    client.connect();
}
```

### 5. 超时配置

```java
// HTTP 客户端超时
HttpClientConfig config = new HttpClientConfig()
    .setConnectTimeoutSeconds(10)
    .setReadTimeoutSeconds(60)
    .setSslHandshakeTimeoutSeconds(30);
```

---

## 附录：文件路径速查表

| 功能 | 路径 |
|------|------|
| **核心API** | |
| Pipeline | `cc.jfire.jnet.common.api.Pipeline` |
| ReadProcessor | `cc.jfire.jnet.common.api.ReadProcessor` |
| WriteProcessor | `cc.jfire.jnet.common.api.WriteProcessor` |
| **服务器/客户端** | |
| AioServer | `cc.jfire.jnet.server.AioServer` |
| ClientChannel | `cc.jfire.jnet.client.ClientChannel` |
| **HTTP** | |
| HttpClient | `cc.jfire.jnet.extend.http.client.HttpClient` |
| HttpRequest | `cc.jfire.jnet.extend.http.dto.HttpRequest` |
| HttpResponse | `cc.jfire.jnet.extend.http.dto.HttpResponse` |
| **WebSocket** | |
| WebSocketFrame | `cc.jfire.jnet.extend.websocket.dto.WebSocketFrame` |
| WebSocketFrameDecoder | `cc.jfire.jnet.extend.websocket.coder.WebSocketFrameDecoder` |
| **缓冲区** | |
| IoBuffer | `cc.jfire.jnet.common.buffer.buffer.IoBuffer` |
| BufferAllocator | `cc.jfire.jnet.common.buffer.allocator.BufferAllocator` |
