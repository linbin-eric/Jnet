# JNet 内置 ReadProcessor 和 WriteProcessor

## ReadProcessor 概述

ReadProcessor 用于处理入站数据，数据从网络读取后依次经过 ReadProcessor 链进行处理。

### 核心接口

```java
public interface ReadProcessor<T> {
    void read(T data, ReadProcessorNode next);
    default void readFailed(Throwable e, ReadProcessorNode next);
    default void readCompleted(ReadProcessorNode next);
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next);
}
```

**关键方法：**
- `read()` - 处理数据，调用 `next.fireRead(data)` 传递给下一个处理器
- `readFailed()` - 处理读取失败
- `readCompleted()` - 单次读取完成后触发
- `pipelineComplete()` - 管道初始化完成时触发

## 内置 ReadProcessor

### 1. AbstractDecoder

所有解码器的基类，提供累积缓冲区机制。

**特性：**
- 自动管理累积缓冲区 `accumulation`
- 数据不足时等待，数据充足时循环解析
- 子类只需实现 `process0(ReadProcessorNode next)` 方法

**使用场景：** 实现自定义协议解码器

**示例：**
```java
public class MyDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        // accumulation 是累积缓冲区
        if (accumulation.remainRead() < 4) {
            return; // 数据不足，等待更多数据
        }

        int length = accumulation.getInt();
        if (accumulation.remainRead() < length) {
            return; // 数据不足
        }

        IoBuffer data = accumulation.slice(length);
        next.fireRead(data); // 传递给下一个处理器
    }
}
```

### 2. HttpRequestPartDecoder

HTTP 请求解码器，将字节流解码为 HTTP 请求片段。

**输入：** `IoBuffer`
**输出：** `HttpRequestPartHead`, `HttpRequestFixLengthBodyPart`, `HttpRequestChunkedBodyPart`

**特性：**
- 状态机解析（REQUEST_LINE → REQUEST_HEADER → BODY）
- 支持固定长度和分块传输编码
- 自动处理请求头和请求体

**使用场景：** 构建 HTTP 服务器

**示例：**
```java
pipeline.addReadProcessor(new HttpRequestPartDecoder());
```

### 3. HttpRequestAggregator

HTTP 请求聚合器，将 HTTP 请求片段聚合为完整的 HttpRequest 对象。

**输入：** `HttpRequestPartHead`, `HttpRequestFixLengthBodyPart`, `HttpRequestChunkedBodyPart`
**输出：** `HttpRequest`

**特性：**
- 自动聚合请求头和请求体
- 处理分块传输编码
- 释放中间缓冲区

**使用场景：** 与 HttpRequestPartDecoder 配合使用

**示例：**
```java
pipeline.addReadProcessor(new HttpRequestPartDecoder());
pipeline.addReadProcessor(new HttpRequestAggregator());
```

### 4. HttpResponsePartDecoder

HTTP 响应解码器，用于客户端解析服务器响应。

**输入：** `IoBuffer`
**输出：** `HttpResponsePartHead`, `HttpResponseFixLengthBodyPart`, `HttpResponseChunkedBodyPart`

**使用场景：** 构建 HTTP 客户端

### 5. WebSocketFrameDecoder

WebSocket 帧解码器，解析 WebSocket 协议帧。

**输入：** `IoBuffer`
**输出：** `WebSocketFrame`

**特性：**
- 支持服务端模式和客户端模式
- 自动处理掩码
- 支持分片消息
- 状态机解析（FRAME_HEADER → EXTENDED_LENGTH → MASKING_KEY → PAYLOAD）

**构造参数：**
- `serverMode` - true=服务端模式（要求 MASK=1），false=客户端模式（要求 MASK=0）

**使用场景：** 构建 WebSocket 服务器或客户端

**示例：**
```java
// 服务端模式
pipeline.addReadProcessor(new WebSocketFrameDecoder(true));

// 客户端模式
pipeline.addReadProcessor(new WebSocketFrameDecoder(false));
```

### 6. WebSocketUpgradeDecoder

WebSocket 升级解码器，处理 HTTP 到 WebSocket 的协议升级。

**输入：** `HttpRequest`
**输出：** `HttpRequest` (透传) 或自动回复 101 响应

**特性：**
- 自动检测 WebSocket 升级请求
- 自动计算 Sec-WebSocket-Accept
- 自动回复 101 Switching Protocols

**使用场景：** WebSocket 服务器

**示例：**
```java
pipeline.addReadProcessor(new HttpRequestPartDecoder());
pipeline.addReadProcessor(new HttpRequestAggregator());
pipeline.addReadProcessor(new WebSocketUpgradeDecoder());
pipeline.addReadProcessor(new WebSocketFrameDecoder(true));
```

### 7. SSLDecoder / ClientSSLDecoder

SSL/TLS 解码器，处理加密数据的解密。

**输入：** `IoBuffer` (加密数据)
**输出：** `IoBuffer` (解密数据)

**特性：**
- 自动处理 SSL 握手
- 支持 TLS 1.2 和 TLS 1.3
- 自动管理 SSLEngine

**使用场景：** HTTPS 服务器或客户端

**示例：**
```java
SSLEngine sslEngine = sslContext.createSSLEngine();
sslEngine.setUseClientMode(false); // 服务端模式
sslEngine.beginHandshake();

pipeline.addReadProcessor(new SSLDecoder(sslEngine));
```

### 8. FixLengthDecoder

固定长度解码器，按固定长度切分数据。

**输入：** `IoBuffer`
**输出：** `IoBuffer` (固定长度)

**构造参数：**
- `length` - 固定长度

**使用场景：** 固定长度协议

### 9. TotalLengthFieldBasedFrameDecoder

基于长度字段的帧解码器，根据消息头中的长度字段切分数据。

**构造参数：**
- `lengthFieldOffset` - 长度字段的偏移量
- `lengthFieldLength` - 长度字段的长度（1/2/4 字节）
- `lengthAdjustment` - 长度调整值
- `initialBytesToStrip` - 跳过的初始字节数

**使用场景：** 自定义协议（如 RPC）

### 10. ValidatedLengthFrameDecoder

安全增强型基于长度的报文解码器，提供魔法值、长度范围和 CRC16 校验。

**输入：** `IoBuffer`
**输出：** `IoBuffer` (报文体)

**报文格式：**
```
+-------------+-----------+--------+----------+
| 魔法值(4B)  | 长度(4B)   | CRC16  | 报文体    |
+-------------+-----------+--------+----------+
```

**特性：**
- 魔法值检查：验证前4字节是否与预设值匹配
- 长度范围校验：验证长度是否在 [minLength, maxLength] 范围内
- CRC16 校验：对魔法值和长度字段计算 CRC16-CCITT 校验和，防止数据篡改
- 失败处理：魔法值不匹配或 CRC 校验失败时关闭连接，长度超限时抛出 TooLongException

**构造参数：**
- `magic` - 4字节魔法值（如 0xCAFEBABE）
- `minLength` - 最小报文体长度（可选，默认0）
- `maxLength` - 最大报文体长度

**使用场景：** 自定义协议（需要防篡改和长度校验）

**示例：**
```java
// 魔法值 0xCAFEBABE，最大报文体 1MB
ValidatedLengthFrameDecoder decoder =
    new ValidatedLengthFrameDecoder(0xCAFEBABE, 1024 * 1024);

// 指定最小和最大长度
ValidatedLengthFrameDecoder decoder =
    new ValidatedLengthFrameDecoder(0xCAFEBABE, 10, 1024 * 1024);

pipeline.addReadProcessor(decoder);
```

### 11. OptionsProcessor

HTTP OPTIONS 请求处理器，自动响应 OPTIONS 请求。

**输入：** `HttpRequest`
**输出：** 自动回复 OPTIONS 响应或透传

**使用场景：** HTTP 服务器（CORS 支持）

### 12. ResourceProcessor

静态资源处理器，自动处理静态文件请求。

**使用场景：** HTTP 文件服务器

## WriteProcessor 概述

WriteProcessor 用于处理出站数据，数据从业务逻辑写出前依次经过 WriteProcessor 链进行处理。

### 核心接口

```java
public interface WriteProcessor<T> {
    default void write(T data, WriteProcessorNode next);
    default void writeFailed(WriteProcessorNode next, Throwable e);
}
```

**关键方法：**
- `write()` - 处理数据，调用 `next.fireWrite(data)` 传递给下一个处理器
- `writeFailed()` - 处理写入失败

## 内置 WriteProcessor

### 1. HttpRespEncoder

HTTP 响应编码器，将 HttpResponse 对象编码为字节流。

**输入：** `HttpResponse`, `HttpResponsePartHead`, `HttpResponseFixLengthBodyPart`, `HttpResponseChunkedBodyPart`, `IoBuffer`
**输出：** `IoBuffer`

**特性：**
- 自动设置 Content-Length 和 Content-Type
- 支持分片响应
- 支持分块传输编码

**使用场景：** HTTP 服务器

**示例：**
```java
pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
```

### 2. WebSocketFrameEncoder

WebSocket 帧编码器，将 WebSocketFrame 编码为字节流。

**输入：** `WebSocketFrame`
**输出：** `IoBuffer`

**特性：**
- 支持客户端模式（自动添加掩码）
- 支持服务端模式（不添加掩码）
- 自动处理帧头

**构造参数：**
- `clientMode` - true=客户端模式（添加掩码），false=服务端模式

**使用场景：** WebSocket 服务器或客户端

**示例：**
```java
// 服务端模式
pipeline.addWriteProcessor(new WebSocketFrameEncoder(false));

// 客户端模式
pipeline.addWriteProcessor(new WebSocketFrameEncoder(true));
```

### 3. SSLEncoder

SSL/TLS 编码器，处理数据的加密。

**输入：** `IoBuffer` (明文数据)
**输出：** `IoBuffer` (加密数据)

**使用场景：** HTTPS 服务器或客户端

**示例：**
```java
SSLEngine sslEngine = sslContext.createSSLEngine();
pipeline.addWriteProcessor(new SSLEncoder(sslEngine));
```

### 4. ValidatedLengthFrameEncoder

安全增强型基于长度的报文编码器，与 ValidatedLengthFrameDecoder 配套使用。

**输入：** `IoBuffer` (报文体)
**输出：** `IoBuffer` (完整报文)

**报文格式：**
```
+-------------+-----------+--------+----------+
| 魔法值(4B)  | 长度(4B)   | CRC16  | 报文体    |
+-------------+-----------+--------+----------+
```

**特性：**
- 自动添加魔法值、长度字段和 CRC16 校验和
- 自动计算报文体长度
- 自动释放输入的报文体 buffer

**构造参数：**
- `magic` - 4字节魔法值（需与解码器一致）
- `allocator` - 缓冲区分配器

**使用场景：** 与 ValidatedLengthFrameDecoder 配套使用

**示例：**
```java
ValidatedLengthFrameEncoder encoder =
    new ValidatedLengthFrameEncoder(0xCAFEBABE, pipeline.allocator());

pipeline.addWriteProcessor(encoder);
```

**完整示例（编解码器配套使用）：**
```java
BufferAllocator allocator = new PooledBufferAllocator();

// 解码器：接收数据并验证
ValidatedLengthFrameDecoder decoder =
    new ValidatedLengthFrameDecoder(0xCAFEBABE, 1024 * 1024);

// 编码器：发送数据时添加报文头
ValidatedLengthFrameEncoder encoder =
    new ValidatedLengthFrameEncoder(0xCAFEBABE, allocator);

// 管道配置
pipeline.addReadProcessor(decoder);
pipeline.addWriteProcessor(encoder);
pipeline.addReadProcessor(myBusinessHandler);
```

### 5. LengthEncoder

长度字段编码器，在消息头中写入长度字段。

**构造参数：**
- `lengthFieldOffset` - 长度字段的偏移量
- `lengthFieldLength` - 长度字段的长度（1/2/4 字节）

**使用场景：** 自定义协议（与 TotalLengthFieldBasedFrameDecoder 配合）

**示例：**
```java
pipeline.addWriteProcessor(new LengthEncoder(0, 4));
```

### 6. CorsEncoder

CORS 响应头编码器，自动添加 CORS 相关响应头。

**使用场景：** HTTP 服务器（跨域支持）

## 处理器链执行流程

### 读取流程

```
网络 → IoBuffer → [ReadProcessor1] → [ReadProcessor2] → ... → 业务逻辑
```

**示例：HTTP 服务器**
```
网络 → IoBuffer
    → HttpRequestPartDecoder (解码 HTTP 片段)
    → HttpRequestAggregator (聚合为完整请求)
    → 业务逻辑 (处理 HttpRequest)
```

### 写入流程

```
业务逻辑 → [WriteProcessor1] → [WriteProcessor2] → ... → IoBuffer → 网络
```

**示例：HTTP 服务器**
```
业务逻辑 (生成 HttpResponse)
    → HttpRespEncoder (编码为 IoBuffer)
    → IoBuffer → 网络
```

## 最佳实践

1. **解码器顺序：** 先协议解码（如 SSL），再应用层解码（如 HTTP）
2. **编码器顺序：** 先应用层编码（如 HTTP），再协议编码（如 SSL）
3. **资源释放：** 解码器必须释放 `accumulation`，编码器必须释放输入 buffer
4. **数据传递：** 调用 `next.fireRead()` 或 `next.fireWrite()` 传递数据
5. **错误处理：** 捕获异常并调用 `pipeline.shutdownInput()` 关闭连接
