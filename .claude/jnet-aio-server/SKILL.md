---
name: jnet-aio-server
description: Guide for using JNet AIO Server to build TCP/HTTP/HTTPS/WebSocket servers with pipeline architecture, and configuring reverse proxy with ReverseApp. Covers creating servers, configuring pipelines, using built-in ReadProcessor and WriteProcessor (decoders/encoders for HTTP, WebSocket, SSL, etc.), writing custom ReadProcessor for protocol parsing, and setting up reverse proxy with SSL certificates. Use when building network servers with JNet, implementing custom protocols, configuring reverse proxy, or working with JNet's pipeline processing model.
---

# JNet AIO Server

## Overview

Use this skill when building network servers with JNet's AIO (Asynchronous I/O) framework, implementing custom protocols, or working with JNet's pipeline architecture.

## Quick Start

### Basic Server Pattern

```java
ChannelConfig config = new ChannelConfig().setPort(8080);

AioServer server = AioServer.newAioServer(config, pipeline -> {
    // 添加 ReadProcessor（入站处理）
    pipeline.addReadProcessor(new MyDecoder());
    pipeline.addReadProcessor((data, next) -> {
        // 业务逻辑
        pipeline.fireWrite(response);
    });

    // 添加 WriteProcessor（出站处理）
    pipeline.addWriteProcessor(new MyEncoder());
});

server.start();
```

**关键概念：**
- `AioServer` - 服务器主类
- `Pipeline` - 处理器链，管理 ReadProcessor 和 WriteProcessor
- `ReadProcessor` - 处理入站数据（网络 → 业务）
- `WriteProcessor` - 处理出站数据（业务 → 网络）

## 核心工作流程

### 1. 创建简单 TCP 服务器

```java
ChannelConfig config = new ChannelConfig().setPort(8080);

AioServer server = AioServer.newAioServer(config, pipeline -> {
    pipeline.addReadProcessor((IoBuffer data, next) -> {
        // 处理接收到的数据
        String text = StandardCharsets.UTF_8
            .decode(data.readableByteBuffer())
            .toString();

        System.out.println("收到: " + text);
        data.free(); // 释放资源
    });
});

server.start();
```

### 2. 创建 HTTP 服务器

```java
AioServer server = AioServer.newAioServer(config, pipeline -> {
    // HTTP 解码器链
    pipeline.addReadProcessor(new HttpRequestPartDecoder());
    pipeline.addReadProcessor(new HttpRequestAggregator());

    // 业务处理
    pipeline.addReadProcessor((HttpRequest request, next) -> {
        HttpResponse response = new HttpResponse();
        response.addHeader("Content-Type", "text/html");
        response.setBodyText("<h1>Hello</h1>");
        pipeline.fireWrite(response);
        request.close();
    });

    // HTTP 编码器
    pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
});
```

### 3. 编写自定义解码器

继承 `AbstractDecoder` 实现自定义协议解析：

```java
public class MyDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        // accumulation 是累积缓冲区
        while (accumulation.remainRead() >= 4) {
            int length = accumulation.getInt();
            if (accumulation.remainRead() < length) {
                return; // 数据不足，等待
            }

            IoBuffer message = accumulation.slice(length);
            next.fireRead(message); // 传递给下一个处理器
        }
    }
}
```

## 内置处理器

### 常用 ReadProcessor

| 处理器 | 输入 | 输出 | 用途 |
|--------|------|------|------|
| HttpRequestPartDecoder | IoBuffer | HttpRequestPart | HTTP 请求解码 |
| HttpRequestAggregator | HttpRequestPart | HttpRequest | HTTP 请求聚合 |
| WebSocketFrameDecoder | IoBuffer | WebSocketFrame | WebSocket 帧解码 |
| WebSocketUpgradeDecoder | HttpRequest | - | WebSocket 协议升级 |
| SSLDecoder | IoBuffer(加密) | IoBuffer(明文) | SSL/TLS 解密 |
| OptionsProcessor | HttpRequest | - | 自动处理 OPTIONS 请求 |

### 常用 WriteProcessor

| 处理器 | 输入 | 输出 | 用途 |
|--------|------|------|------|
| HttpRespEncoder | HttpResponse | IoBuffer | HTTP 响应编码 |
| WebSocketFrameEncoder | WebSocketFrame | IoBuffer | WebSocket 帧编码 |
| SSLEncoder | IoBuffer(明文) | IoBuffer(加密) | SSL/TLS 加密 |
| LengthEncoder | IoBuffer | IoBuffer | 写入长度字段 |

**详细说明：** 参见 `references/processors.md`

## 编写自定义 ReadProcessor

### 基础模式

```java
public class SimpleProcessor implements ReadProcessor<String> {
    @Override
    public void read(String data, ReadProcessorNode next) {
        // 处理数据
        System.out.println("收到: " + data);

        // 传递给下一个处理器
        next.fireRead(data);
    }
}
```

### 解码器模式

使用 `AbstractDecoder` 基类：

```java
public class LengthFieldDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        while (true) {
            // 检查数据是否充足
            if (accumulation.remainRead() < 4) {
                return; // 等待更多数据
            }

            int length = accumulation.getInt(accumulation.getReadPosi());

            if (accumulation.remainRead() < 4 + length) {
                return; // 等待更多数据
            }

            // 解析消息
            accumulation.skip(4);
            IoBuffer message = accumulation.slice(length);
            next.fireRead(message);
        }
    }
}
```

**详细指南：** 参见 `references/custom-processor.md`

## Pipeline 架构

### 处理器链执行流程

**入站（读取）：**
```
网络 → IoBuffer → [Decoder1] → [Decoder2] → 业务逻辑
```

**出站（写入）：**
```
业务逻辑 → [Encoder1] → [Encoder2] → IoBuffer → 网络
```

### 处理器顺序

**读处理器（从外到内）：**
1. 协议层（SSL 解密）
2. 应用层（HTTP 解码）
3. 功能层（OPTIONS 处理）
4. 业务层（业务逻辑）

**写处理器（从内到外）：**
1. 业务层（业务逻辑）
2. 应用层（HTTP 编码）
3. 协议层（SSL 加密）

## 常见场景

### HTTP 服务器

参见 `assets/http-server.java` - 完整的 HTTP 服务器模板，支持路由和错误处理

### WebSocket 服务器

```java
pipeline.addReadProcessor(new HttpRequestPartDecoder());
pipeline.addReadProcessor(new HttpRequestAggregator());
pipeline.addReadProcessor(new WebSocketUpgradeDecoder());
pipeline.addReadProcessor(new WebSocketFrameDecoder(true)); // 服务端模式

pipeline.addReadProcessor((WebSocketFrame frame, next) -> {
    // 处理 WebSocket 消息
});

pipeline.addWriteProcessor(new WebSocketFrameEncoder(false)); // 服务端模式
```

### HTTPS 服务器

```java
SSLEngine sslEngine = sslContext.createSSLEngine();
sslEngine.setUseClientMode(false);
sslEngine.beginHandshake();

pipeline.addReadProcessor(new SSLDecoder(sslEngine));
// ... HTTP 处理器
pipeline.addWriteProcessor(new SSLEncoder(sslEngine));
```

### 自定义协议服务器

参见 `assets/custom-decoder.java` - 包含三种解码器模式：
- 基于长度字段
- 基于分隔符
- 状态机解码器

### 反向代理服务器

使用 ReverseApp 快速搭建反向代理：

```yaml
# reverse.config
8080:
  /static:
    type: resource
    path: file:/var/www/static
    order: 2
  /api/*:
    type: proxy
    path: http://backend:8080/
    order: 1
  ssl:
    enable: true
    cert: /path/to/keystore.jks
    password: your_password
```

启动：`java -jar jnet-app.jar`

**详细配置：** 参见 `references/reverse-proxy.md`

## 资源管理

**关键规则：**
1. **IoBuffer 必须释放：** 使用完 IoBuffer 后调用 `buffer.free()`
2. **HttpRequest/HttpResponse 必须关闭：** 调用 `request.close()` 或 `response.close()`
3. **解码器累积缓冲区：** `AbstractDecoder` 自动管理 `accumulation`
4. **错误时关闭连接：** 调用 `pipeline.shutdownInput()`

## Pipeline 高级功能

### 连接附件

在连接上存储状态：

```java
pipeline.setAttach(new SessionData());
SessionData session = (SessionData) pipeline.getAttach();
```

### 持久化存储

在处理器间共享数据：

```java
pipeline.putPersistenceStore("key", value);
Object value = pipeline.getPersistenceStore("key");
```

### 获取连接信息

```java
AsynchronousSocketChannel channel = pipeline.socketChannel();
String remoteAddress = pipeline.getRemoteAddressWithoutException();
BufferAllocator allocator = pipeline.allocator();
```

## Resources

### references/
- **processors.md** - 所有内置 ReadProcessor 和 WriteProcessor 的详细说明
- **custom-processor.md** - 编写自定义 ReadProcessor 的完整指南
- **examples.md** - 各种服务器场景的完整示例
- **reverse-proxy.md** - 反向代理配置指南（ReverseApp、SSL 证书配置）

### assets/
- **basic-tcp-server.java** - 基础 TCP Echo 服务器模板
- **http-server.java** - 完整的 HTTP 服务器模板（支持路由）
- **custom-decoder.java** - 三种自定义解码器模式

## 最佳实践

1. **处理器单一职责** - 每个处理器只负责一个任务
2. **正确的处理器顺序** - 先协议层，再应用层，最后业务层
3. **资源及时释放** - IoBuffer 使用完立即 `free()`
4. **循环解析** - 解码器中使用 `while` 循环尽可能多地解析数据
5. **数据不足时返回** - 解码器数据不足时直接 `return`，等待更多数据
6. **错误处理** - 捕获异常并调用 `pipeline.shutdownInput()`
7. **避免阻塞** - 不要在处理器中执行阻塞操作

## 何时阅读 References

- **processors.md** - 需要了解内置处理器的详细功能和参数
- **custom-processor.md** - 需要编写自定义协议解码器
- **examples.md** - 需要完整的服务器实现示例
- **reverse-proxy.md** - 需要配置反向代理或 SSL 证书

## 快速参考

```java
// 创建服务器
AioServer server = AioServer.newAioServer(config, pipeline -> {
    // 添加处理器
});
server.start();

// ReadProcessor
void read(T data, ReadProcessorNode next);
next.fireRead(data);

// WriteProcessor
void write(T data, WriteProcessorNode next);
next.fireWrite(data);

// AbstractDecoder
protected void process0(ReadProcessorNode next);
accumulation.remainRead();
accumulation.slice(length);

// Pipeline
pipeline.fireWrite(data);
pipeline.allocator();
pipeline.shutdownInput();
```
