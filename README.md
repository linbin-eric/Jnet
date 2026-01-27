# JNet

JNet 是一个基于 Java AIO (异步 I/O) 构建的高性能、异步网络通信框架。专为构建可扩展的 TCP 客户端和服务器而设计，强调模块化和数据处理管道，使其易于实现复杂的协议和业务逻辑。

## 特性

- **异步非阻塞**: 基于 Java AIO 构建，使用虚拟线程，操作不会阻塞线程，支持高并发
- **管道架构**: 通过处理器链实现模块化、可组合的数据处理
- **显式流程控制**: 处理器显式调用链中的下一步，提供精细的数据流和错误处理控制
- **池化缓冲区**: 内置高性能的内存池化 Buffer 分配器，减少 GC 压力
- **SSL/TLS 支持**: 内置 SSL 编解码器，轻松构建安全通信
- **HTTP 支持**: 提供 HTTP 请求解码和响应编码器
- **反向代理**: 内置可配置的反向代理应用
- **背压控制**: 内置流量控制机制，防止生产者速度过快导致内存溢出

## 环境要求

- Java 21+
- Maven 3.x

## 快速开始

### Maven 依赖

```xml
<dependency>
    <groupId>cc.jfire</groupId>
    <artifactId>Jnet</artifactId>
    <version>1.0</version>
</dependency>
```

### 创建 Echo 服务器

```java
import cc.jfire.jnet.common.api.*;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;

public class EchoServer {
    public static void main(String[] args) {
        // 1. 配置服务器
        ChannelConfig config = new ChannelConfig();
        config.setPort(8080);

        // 2. 定义管道初始化器
        PipelineInitializer initializer = pipeline -> {
            pipeline.addReadProcessor(new ReadProcessor<Object>() {
                @Override
                public void read(Object data, ReadProcessorNode next) {
                    System.out.println("收到: " + data);
                    // 回写数据
                    pipeline.fireWrite(data);
                    // 继续处理
                    next.fireRead(data);
                }
            });
        };

        // 3. 创建并启动服务器
        AioServer server = AioServer.newAioServer(config, initializer);
        server.start();
        System.out.println("Echo 服务器已在端口 8080 启动");
    }
}
```

### 创建 TCP 客户端

```java
import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.*;
import cc.jfire.jnet.common.util.ChannelConfig;

public class EchoClient {
    public static void main(String[] args) {
        // 1. 配置客户端
        ChannelConfig config = new ChannelConfig();
        config.setIp("127.0.0.1");
        config.setPort(8080);

        // 2. 定义管道初始化器
        PipelineInitializer initializer = pipeline -> {
            pipeline.addReadProcessor(new ReadProcessor<Object>() {
                @Override
                public void read(Object data, ReadProcessorNode next) {
                    System.out.println("收到回复: " + data);
                    next.fireRead(data);
                }
            });
        };

        // 3. 创建并连接
        ClientChannel client = ClientChannel.newClient(config, initializer);
        if (client.connect()) {
            System.out.println("已连接到服务器");
            client.pipeline().fireWrite("Hello, Server!");
        } else {
            System.err.println("连接失败: " + client.getConnectionException());
        }
    }
}
```

### 创建 HTTPS 服务器

```java
import cc.jfire.jnet.common.api.*;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.server.AioServer;

import javax.net.ssl.*;
import java.security.KeyStore;

public class HttpsServer
{
    public static void main(String[] args) throws Exception
    {
        ChannelConfig config = new ChannelConfig().setPort(8443);
        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 初始化 SSL
            KeyStore keyStore = KeyStore.getInstance("JKS");
            keyStore.load(getClass().getResourceAsStream("/keystore.jks"), "password".toCharArray());
            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(keyStore, "password".toCharArray());
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(kmf.getKeyManagers(), null, null);
            SSLEngine sslEngine = sslContext.createSSLEngine();
            sslEngine.setUseClientMode(false);
            sslEngine.beginHandshake();
            SSLDecoder sslDecoder = new SSLDecoder(sslEngine);
            SSLEncoder sslEncoder = new SSLEncoder(sslEngine, sslDecoder);
            // 添加处理器链
            pipeline.addReadProcessor(sslDecoder);
            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());
            pipeline.addReadProcessor(new ReadProcessor<HttpRequest>()
            {
                @Override
                public void read(HttpRequest request, ReadProcessorNode next)
                {
                    request.close();
                    HttpResponse resp = new HttpResponse();
                    resp.addHeader("Content-Type", "text/html");
                    resp.setBodyText("Hello, HTTPS!");
                    next.pipeline().fireWrite(resp);
                }
            });
            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
            pipeline.addWriteProcessor(sslEncoder);
        });
        server.start();
    }
}
```

## 核心概念

### Pipeline (管道)

Pipeline 是 JNet 的核心，代表一个网络连接，管理数据流。数据通过处理器链进行处理：

- **入站 (Read)**: 接收到的数据流经 `ReadProcessor` 链
- **出站 (Write)**: 发送的数据流经 `WriteProcessor` 链

```java
public interface Pipeline {
    void fireWrite(Object data);                    // 发起出站数据流
    void addReadProcessor(ReadProcessor<?> p);      // 添加读处理器
    void addWriteProcessor(WriteProcessor<?> p);    // 添加写处理器
    void shutdownInput();                           // 关闭连接
    boolean isOpen();                               // 检查连接状态
}
```

### ReadProcessor (读处理器)

处理入站数据：

```java
public interface ReadProcessor<T> {
    void read(T data, ReadProcessorNode next);
    default void readFailed(Throwable e, ReadProcessorNode next);
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next);
}
```

### WriteProcessor (写处理器)

处理出站数据：

```java
public interface WriteProcessor<T> {
    default void write(T data, WriteProcessorNode next);
    default void writeFailed(WriteProcessorNode next, Throwable e);
}
```

### ChannelConfig (通道配置)

配置网络通道的参数：

```java
ChannelConfig config = new ChannelConfig()
    .setIp("0.0.0.0")           // 绑定 IP
    .setPort(8080)              // 绑定端口
    .setBackLog(50)             // 连接队列大小
    .setInitReceiveSize(1024)   // 初始接收缓冲区大小
    .setMaxReceiveSize(8 * 1024 * 1024); // 最大接收缓冲区大小
```

## 内置组件

### 编解码器

| 类名 | 说明 |
|------|------|
| `TotalLengthFieldBasedFrameDecoder` | 基于长度字段的报文解码器 |
| `HeadCheckTotalLengthDecoder` | 带头部校验的长度解码器 |
| `FixLengthDecoder` | 固定长度解码器 |
| `LengthEncoder` | 长度编码器 |

### HTTP 组件

| 类名 | 说明 |
|------|------|
| `HttpReqPartDecoder` | HTTP 分段解码器（支持 chunked） |
| `AggregationHttpReqDecoder` | HTTP 请求聚合器（组装 HttpRequest） |
| `HttpRespEncoder` | HTTP 响应编码器 |
| `SSLDecoder` / `SSLEncoder` | SSL/TLS 编解码器 |

### 工具类

| 类名 | 说明 |
|------|------|
| `HeartBeat` | 心跳检测处理器 |
| `IoBuffer` | 高性能缓冲区接口 |

### 背压控制

| 类名 | 说明 |
|------|------|
| `BackPresure` | 背压控制器工厂，创建读写限流器组合 |
| `NoticeReadLimiter` | 通知式读限流器，水位下降时恢复读取 |
| `NoticeWriteLimiter` | 通知式写限流器，监控写入量并触发恢复 |
| `BusyWaitReadLimiter` | 忙等待式读限流器，自旋等待水位下降 |
| `BusyWaitWriteLimiter` | 忙等待式写限流器，跟踪待写入数据量 |

## 背压控制

背压（Back Pressure）是一种流量控制机制，用于防止生产者发送数据的速度超过消费者处理数据的速度，从而避免内存溢出。JNet 提供了两种背压策略：

### 核心组件

背压控制由以下组件协同工作：

| 组件 | 类型 | 作用 |
|------|------|------|
| `BackPresure` | Record | 背压控制器容器，封装计数器、读限流器、写限流器和阈值 |
| `ReadLimiter` | ReadProcessor | 读处理器，在 `readCompleted()` 时检查水位，决定是否继续读取 |
| `WriteLimiter` | WriteListener | 写监听器，跟踪数据入队和写出完成，维护水位计数器 |

### 工作原理

```
数据入站 → 业务处理 → 数据入队写出
                         ↓
              WriteLimiter.queuedWrite()
              counter += 数据大小
                         ↓
              检查 counter >= limit ?
              ├─ 是 → ReadLimiter 暂停读取
              └─ 否 → 继续读取
                         ↓
              数据写出到网络完成
                         ↓
              WriteLimiter.partWriteFinish()
              counter -= 已写出大小
                         ↓
              检查 counter < limit ?
              └─ 是 → ReadLimiter 恢复读取
```

### 通知式背压（推荐）

通知式背压使用事件驱动机制，当水位下降时主动通知恢复读取，CPU 占用低：

```java
import cc.jfire.jnet.extend.watercheck.BackPresure;
import cc.jfire.jnet.common.api.*;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;

public class BackPressureServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 1. 创建背压控制器，设置水位阈值（如 1MB）
            BackPresure backPresure = BackPresure.noticeWaterLevel(1024 * 1024);

            // 2. 添加业务处理器
            pipeline.addReadProcessor(new ReadProcessor<Object>() {
                @Override
                public void read(Object data, ReadProcessorNode next) {
                    // 处理数据并写出响应
                    pipeline.fireWrite(processData(data));
                    next.fireRead(data);
                }
            });

            // 3. 添加读限流器（放在处理器链末尾）
            pipeline.addReadProcessor(backPresure.readLimiter());

            // 4. 设置写监听器
            pipeline.setWriteListener(backPresure.writeLimiter());
        });

        server.start();
    }
}
```

**关键点：**
- `readLimiter()` 返回的是 `ReadProcessor<Void>`，它在 `readCompleted()` 方法中检查水位
- `writeLimiter()` 返回的是 `WriteListener`，它监听数据入队和写出完成事件
- 两者共享同一个 `AtomicInteger` 计数器

### 忙等待式背压

忙等待式背压使用自旋等待机制，当水位超限时阻塞当前线程直到水位下降。适用于对延迟敏感的场景：

```java
import cc.jfire.jnet.extend.watercheck.BusyWaitReadLimiter;
import cc.jfire.jnet.extend.watercheck.BusyWaitWriteLimiter;
import java.util.concurrent.atomic.AtomicInteger;

public class BusyWaitBackPressureServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 1. 创建共享计数器和阈值
            AtomicInteger counter = new AtomicInteger();
            int limit = 1024 * 1024; // 1MB 水位阈值

            // 2. 添加业务处理器
            pipeline.addReadProcessor(new ReadProcessor<Object>() {
                @Override
                public void read(Object data, ReadProcessorNode next) {
                    pipeline.fireWrite(processData(data));
                    next.fireRead(data);
                }
            });

            // 3. 添加忙等待读限流器
            pipeline.addReadProcessor(new BusyWaitReadLimiter(counter, limit));

            // 4. 设置忙等待写限流器
            pipeline.setWriteListener(new BusyWaitWriteLimiter(counter));
        });

        server.start();
    }
}
```

**忙等待策略：**
- 先进行 16 次 CPU 自旋（`Thread.onSpinWait()`）
- 然后进行 `LockSupport.park()` 等待，时间逐步增加：50ms → 100ms → 1s

### 反向代理中的双向背压

在反向代理场景中，需要同时控制前端和后端的数据流。JNet 使用双向背压机制：

```java
import cc.jfire.jnet.extend.watercheck.BackPresure;

public class ProxyWithBackPressure {
    public static void main(String[] args) {
        // 创建两个背压控制器
        BackPresure inBackPresure = BackPresure.noticeWaterLevel(100 * 1024 * 1024);       // 入站背压
        BackPresure upstreamBackPresure = BackPresure.noticeWaterLevel(100 * 1024 * 1024); // 上游背压

        // 前端连接配置
        AioServer server = AioServer.newAioServer(config, frontendPipeline -> {
            // 前端读处理器链
            frontendPipeline.addReadProcessor(new HttpRequestPartDecoder());
            frontendPipeline.addReadProcessor(new ProxyHandler(backendPipeline -> {
                // 后端连接配置
                // 后端读限流器使用前端的上游背压（控制后端响应速度）
                backendPipeline.addReadProcessor(upstreamBackPresure.readLimiter());
                // 后端写监听器使用前端的入站背压（控制向后端发送速度）
                backendPipeline.setWriteListener(inBackPresure.writeLimiter());
            }));
            // 前端读限流器使用入站背压
            frontendPipeline.addReadProcessor(inBackPresure.readLimiter());
            // 前端写监听器使用上游背压
            frontendPipeline.setWriteListener(upstreamBackPresure.writeLimiter());
        });
    }
}
```

**双向背压流程：**
```
客户端 ←→ [前端连接] ←→ [后端连接] ←→ 后端服务器

入站背压 (inBackPresure):
  - 前端 readLimiter: 控制从客户端读取的速度
  - 后端 writeLimiter: 监控向后端写入的数据量

上游背压 (upstreamBackPresure):
  - 后端 readLimiter: 控制从后端读取响应的速度
  - 前端 writeLimiter: 监控向客户端写入的数据量
```

### 背压策略对比

| 策略 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| 通知式 | CPU 占用低，事件驱动，无阻塞 | 恢复有微小延迟 | 一般场景（推荐） |
| 忙等待式 | 恢复延迟极低，响应迅速 | CPU 占用较高，会阻塞线程 | 低延迟敏感场景 |

### 阈值设置建议

| 场景 | 建议阈值 | 说明 |
|------|----------|------|
| 普通 TCP 服务 | 64KB - 1MB | 平衡内存和吞吐量 |
| HTTP 服务 | 1MB - 10MB | 考虑请求/响应体大小 |
| 反向代理 | 10MB - 100MB | 需要缓冲大量转发数据 |
| 文件传输 | 100MB+ | 大文件需要更大缓冲区 |

## HTTP 客户端

JNet 提供了高性能的 HTTP 客户端，具有以下特性：

- **线程安全**: HttpClient 是线程安全的，**全局只需创建一个实例**，可在多线程环境下并发使用
- **内置连接池**: 自动管理连接复用，无需手动管理连接生命周期
- **自动 Keep-Alive**: 连接自动保持活跃，减少连接建立开销
- **代理支持**: 支持 HTTP 代理和 HTTPS CONNECT 隧道
- **流式响应**: 支持流式处理大文件下载或 SSE 场景

### 推荐用法

**重要**: HttpClient 内置连接池，建议在应用中创建单例使用：

```java
// 推荐：创建单例，整个应用共享
public class MyApp {
    // 全局单例，线程安全，支持并发调用
    private static final HttpClient HTTP_CLIENT = new HttpClient(
        new HttpClientConfig()
            .setMaxConnectionsPerHost(100)
            .setKeepAliveSeconds(1800)
    );

    public static HttpClient getHttpClient() {
        return HTTP_CLIENT;
    }
}
```

### 基本用法

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;

public class HttpClientExample {
    // 创建单例客户端
    private static final HttpClient client = new HttpClient();

    public static void main(String[] args) throws Exception {
        // 创建请求
        HttpRequest request = new HttpRequest()
            .setUrl("https://api.example.com/users")
            .get()
            .addHeader("Accept", "application/json");

        // 发送请求
        HttpResponse response = client.call(request);

        System.out.println("状态码: " + response.getStatusCode());
        System.out.println("响应体: " + response.getBodyText());

        // 释放资源
        response.close();
    }
}
```

### 发送 POST 请求

```java
HttpClient client = new HttpClient();

HttpRequest request = new HttpRequest()
    .setUrl("https://api.example.com/users")
    .post()
    .setContentType("application/json")
    .setBody("{\"name\": \"张三\", \"age\": 25}");

HttpResponse response = client.call(request);
response.close();
```

### 配置客户端

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.HttpClientConfig;

public class ConfiguredHttpClient {
    public static void main(String[] args) throws Exception {
        // 创建自定义配置
        HttpClientConfig config = new HttpClientConfig()
            .setConnectTimeoutSeconds(10)      // 连接超时 10 秒
            .setReadTimeoutSeconds(60)         // 读取超时 60 秒
            .setKeepAliveSeconds(1800)         // Keep-Alive 30 分钟
            .setMaxConnectionsPerHost(50)      // 每主机最大连接数
            .setAcquireTimeoutSeconds(5)       // 获取连接超时 5 秒
            .setSslHandshakeTimeoutSeconds(30); // SSL 握手超时 30 秒

        // 创建自定义客户端
        HttpClient client = new HttpClient(config);

        // 发送请求
        HttpRequest request = new HttpRequest()
            .setUrl("https://api.example.com/data")
            .get();

        HttpResponse response = client.call(request);
        response.close();
    }
}
```

### 配置代理

```java
HttpClientConfig config = new HttpClientConfig()
    .setProxyHost("proxy.example.com")
    .setProxyPort(8080);

HttpClient client = new HttpClient(config);

// HTTP 请求通过代理直接转发
HttpRequest httpRequest = new HttpRequest()
    .setUrl("http://api.example.com/data")
    .get();
HttpResponse httpResponse = client.call(httpRequest);

// HTTPS 请求通过 CONNECT 隧道
HttpRequest httpsRequest = new HttpRequest()
    .setUrl("https://api.example.com/secure")
    .get();
HttpResponse httpsResponse = client.call(httpsRequest);
```

### 流式响应处理

对于大文件下载或 Server-Sent Events (SSE) 等场景，可以使用流式响应：

```java
import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.StreamableResponseFuture;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;
import cc.jfire.jnet.extend.http.dto.HttpResponseBodyPart;

public class StreamingExample {
    private static final HttpClient client = new HttpClient();

    public static void main(String[] args) throws Exception {
        HttpRequest request = new HttpRequest()
            .setUrl("https://api.example.com/stream")
            .get();

        // 流式调用
        StreamableResponseFuture future = client.streamCall(
            request,
            // 响应分片处理器
            part -> {
                if (part instanceof HttpResponsePartHead head) {
                    System.out.println("状态码: " + head.getStatusCode());
                } else if (part instanceof HttpResponseBodyPart body) {
                    System.out.println("收到数据块: " + body.getContent().readableBytes() + " 字节");
                    body.free(); // 释放缓冲区
                }
                if (part.isLast()) {
                    System.out.println("响应接收完成");
                }
            },
            // 错误处理器
            error -> {
                System.err.println("请求失败: " + error.getMessage());
            }
        );
    }
}
```

### HttpClient 配置参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| `connectTimeoutSeconds` | 10 | 连接超时时间（秒） |
| `readTimeoutSeconds` | 60 | 读取超时时间（秒） |
| `keepAliveSeconds` | 1800 | Keep-Alive 时间（秒） |
| `maxConnectionsPerHost` | 50 | 每主机最大连接数 |
| `acquireTimeoutSeconds` | 1 | 从连接池获取连接的超时时间（秒） |
| `sslHandshakeTimeoutSeconds` | 30 | SSL 握手超时时间（秒） |
| `proxyHost` | null | 代理服务器主机名 |
| `proxyPort` | 0 | 代理服务器端口 |

### 连接池机制

HttpClient 内置连接池，自动管理连接的复用和回收：

- **连接复用**: 相同主机的请求会复用已建立的连接
- **自动清理**: 失效连接会被自动检测并移除
- **并发控制**: 通过信号量控制每主机的最大连接数
- **超时获取**: 连接池满时会等待可用连接，超时则抛出异常

```java
// 获取连接池状态
HttpClient client = new HttpClient(config);
// 连接池由 HttpClient 内部管理，无需手动操作
```

## 反向代理应用

JNet 内置了一个可配置的反向代理应用 `ReverseApp`。

### 启动方式

```bash
java -jar ReverseApp.jar
```

### 配置文件 (reverse.config)

```yaml
# 监听端口
10180:
  # SSL 配置 (可选)
  ssl:
    enable: false
    cert: keystore.jks
    password: 123456

  # 路由规则
  /js/*:
    type: proxy
    path: http://127.0.0.1:10081/js/

  /api/drg/cloud:
    type: proxy
    path: http://127.0.0.1:10081
    order: 1

  /api/*:
    type: proxy
    path: http://127.0.0.1:10086/
    order: 2

  /:
    type: resource
    path: file:dist
    order: 4
```

### 配置说明

- `type: proxy` - 代理转发
- `type: resource` - 静态资源服务
- `order` - 匹配优先级，数字越小优先级越高
- 路径以 `*` 结尾表示前缀匹配

## 项目结构

```
src/main/java/cc/jfire/jnet/
├── client/           # 客户端实现
├── server/           # 服务端实现
├── common/
│   ├── api/          # 核心接口 (Pipeline, Processor 等)
│   ├── buffer/       # 缓冲区实现
│   ├── coder/        # 编解码器
│   ├── internal/     # 内部实现
│   └── util/         # 工具类
└── extend/
    ├── http/         # HTTP 支持
    ├── reverse/      # 反向代理
    └── watercheck/   # 流量控制
```

## 构建

```bash
mvn clean package
```

## 许可证

请参阅项目许可证文件。
