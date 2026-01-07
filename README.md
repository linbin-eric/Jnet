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
