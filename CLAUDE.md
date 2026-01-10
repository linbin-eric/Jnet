# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

JNet 是一个基于 Java AIO (异步 I/O) 构建的高性能网络通信框架，支持 TCP 客户端/服务器、HTTP/HTTPS、WebSocket 和反向代理功能。

## 构建命令

```bash
# 编译项目
mvn compile

# 打包（跳过测试）
mvn clean package -DskipTests

# 运行测试
mvn test

# 发布到中央仓库（需要 GPG 签名）
mvn clean deploy -Drelease
```

## 核心架构

### 包组织结构

```
cc.jfire.jnet/
├── common/           # 核心通用功能
│   ├── api/          # 核心接口：Pipeline, ReadProcessor, WriteProcessor
│   ├── buffer/       # IoBuffer 缓冲区系统（池化/非池化）
│   ├── coder/        # 基础编解码器（AbstractDecoder）
│   └── internal/     # DefaultPipeline 等内部实现
├── extend/           # 扩展功能
│   ├── http/         # HTTP 协议支持
│   ├── websocket/    # WebSocket 协议支持
│   ├── reverse/      # 反向代理
│   └── watercheck/   # 流量控制/背压
├── server/           # AioServer 服务端
└── client/           # ClientChannel 客户端
```

### 管道模式 (Pipeline Pattern)

JNet 的核心是管道架构，数据通过处理器链进行处理：

```
入站: IoBuffer → [ReadProcessor1] → [ReadProcessor2] → ... → 业务逻辑
出站: 业务数据 → [WriteProcessor1] → [WriteProcessor2] → ... → IoBuffer
```

关键接口：
- `Pipeline` - 管道接口，管理处理器链
- `ReadProcessor<T>` - 读处理器，处理入站数据
- `WriteProcessor<T>` - 写处理器，处理出站数据
- `ReadProcessorNode` / `WriteProcessorNode` - 处理器节点，调用 `next.fireRead()` / `next.fireWrite()` 传递数据

### 编解码器设计

编解码器继承 `AbstractDecoder`，使用状态机模式：

```java
public class MyDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        // accumulation 是累积缓冲区
        // 解析完成后调用 next.fireRead(parsedObject)
    }
}
```

### 缓冲区系统

- `IoBuffer` - 缓冲区接口，支持自动扩容、切片(slice)、压缩(compact)
- `PooledBufferAllocator` - 池化分配器（推荐），基于 Arena+Chunk+SubPage 架构
- `UnPoolBufferAllocator` - 非池化分配器

重要方法：
- `allocator.allocate(size)` - 分配缓冲区
- `buffer.free()` - 释放缓冲区（池化时回收）
- `buffer.slice(length)` - 切片
- `buffer.compact()` - 压缩已读数据

## 主要模块

### HTTP 模块 (`extend/http/`)

- `HttpRequestPartDecoder` - HTTP 请求解码器（状态机：REQUEST_LINE → REQUEST_HEADER → BODY）
- `HttpRequestAggregator` - 请求聚合器（将分段组装为完整 HttpRequest）
- `HttpRespEncoder` - HTTP 响应编码器
- `SSLDecoder` / `SSLEncoder` - SSL/TLS 支持

### WebSocket 模块 (`extend/websocket/`)

- `WebSocketUpgradeDecoder` - HTTP 升级到 WebSocket（自动回复 101）
- `WebSocketFrameDecoder` - WebSocket 帧解码器（RFC 6455，状态机解析）
- `WebSocketFrameEncoder` - WebSocket 帧编码器
- `WebSocketFrame` - 帧 DTO（支持 TEXT/BINARY/PING/PONG/CLOSE）

### 反向代理 (`extend/reverse/`)

- `ReverseProxyServer` - 反向代理服务器
- `TransferProcessor` - 转发处理器
- 配置文件：`reverse.config`

## 开发注意事项

1. **Java 版本**: 要求 Java 21+（使用虚拟线程）

2. **资源释放**: IoBuffer 使用完毕后必须调用 `free()` 释放，HttpRequest/HttpResponse 需调用 `close()`

3. **处理器链传递**: 在 ReadProcessor/WriteProcessor 中必须显式调用 `next.fireRead()` / `next.fireWrite()` 传递数据

4. **状态机解码**: 解码器使用状态机模式，需处理数据不足时的等待和数据充足时的循环解析

5. **WebSocket 模式**:
   - 服务端模式 (`serverMode=true`): 要求客户端帧 MASK=1
   - 客户端模式 (`clientMode=true`): 发送时添加掩码

## 测试

测试位于 `src/test/java/cc/jfire/jnet/`，缓冲区系统有详细的单元测试和性能基准测试。
