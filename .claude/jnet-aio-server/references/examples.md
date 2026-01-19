# JNet AIO Server 使用示例

## 目录

- [基础 TCP 服务器](#基础-tcp-服务器)
- [HTTP 服务器](#http-服务器)
- [HTTPS 服务器](#https-服务器)
- [WebSocket 服务器](#websocket-服务器)
- [自定义协议服务器](#自定义协议服务器)
- [使用连接附件](#使用连接附件)
- [处理器链组合](#处理器链组合)

## 基础 TCP 服务器

最简单的 TCP 服务器，接收原始字节流。

```java
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;
import java.nio.charset.StandardCharsets;

public class BasicTcpServer {
    public static void main(String[] args) {
        // 配置服务器端口
        ChannelConfig config = new ChannelConfig().setPort(8080);

        // 创建服务器
        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 添加读处理器
            pipeline.addReadProcessor(new ReadProcessor<IoBuffer>() {
                @Override
                public void read(IoBuffer data, ReadProcessorNode next) {
                    // 读取数据
                    String text = StandardCharsets.UTF_8
                        .decode(data.readableByteBuffer())
                        .toString();

                    System.out.println("收到数据: " + text);

                    // 释放 buffer
                    data.free();

                    // 回复数据
                    String response = "Echo: " + text;
                    IoBuffer responseBuffer = pipeline.allocator().allocate(response.length());
                    responseBuffer.put(response.getBytes(StandardCharsets.UTF_8));
                    pipeline.fireWrite(responseBuffer);
                }
            });
        });

        // 启动服务器
        server.start();
        System.out.println("TCP 服务器已启动，监听端口 8080");
    }
}
```

## HTTP 服务器

完整的 HTTP 服务器示例。

```java
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.server.AioServer;

public class HttpServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 添加 HTTP 解码器
            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());
            pipeline.addReadProcessor(new OptionsProcessor());

            // 添加业务处理器
            pipeline.addReadProcessor(new ReadProcessor<HttpRequest>() {
                @Override
                public void read(HttpRequest request, ReadProcessorNode next) {
                    System.out.println("收到请求: " + request.getHead().getMethod()
                        + " " + request.getHead().getPath());

                    // 创建响应
                    HttpResponse response = new HttpResponse();
                    response.setStatusCode(200);
                    response.addHeader("Content-Type", "text/html; charset=utf-8");
                    response.setBodyText("<h1>Hello from JNet!</h1>");

                    // 发送响应
                    pipeline.fireWrite(response);

                    // 释放请求资源
                    request.close();
                }
            });

            // 添加 HTTP 编码器
            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
        });

        server.start();
        System.out.println("HTTP 服务器已启动: http://localhost:8080");
    }
}
```

## HTTPS 服务器

支持 SSL/TLS 的 HTTPS 服务器。

```java
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.server.AioServer;
import javax.net.ssl.*;
import java.io.InputStream;
import java.security.KeyStore;

public class HttpsServer {
    public static void main(String[] args) throws Exception {
        ChannelConfig config = new ChannelConfig().setPort(8443);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            try {
                // 1. 加载密钥库
                KeyStore keyStore = KeyStore.getInstance("JKS");
                try (InputStream is = HttpsServer.class.getResourceAsStream("/keystore.jks")) {
                    keyStore.load(is, "password".toCharArray());
                }

                // 2. 初始化 KeyManagerFactory
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(
                    KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, "password".toCharArray());

                // 3. 初始化 TrustManagerFactory
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);

                // 4. 初始化 SSLContext
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);

                // 5. 创建 SSLEngine
                SSLEngine sslEngine = sslContext.createSSLEngine();
                sslEngine.setUseClientMode(false);
                sslEngine.setNeedClientAuth(false);
                sslEngine.setEnabledProtocols(new String[]{"TLSv1.2", "TLSv1.3"});
                sslEngine.beginHandshake();

                // 添加 SSL 处理器
                SSLDecoder sslDecoder = new SSLDecoder(sslEngine);
                SSLEncoder sslEncoder = new SSLEncoder(sslEngine);

                pipeline.addReadProcessor(sslDecoder);
                pipeline.addReadProcessor(new HttpRequestPartDecoder());
                pipeline.addReadProcessor(new HttpRequestAggregator());
                pipeline.addReadProcessor((HttpRequest request, next) -> {
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", "text/html");
                    response.setBodyText("<h1>Secure HTTPS Server</h1>");
                    pipeline.fireWrite(response);
                    request.close();
                });

                pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
                pipeline.addWriteProcessor(sslEncoder);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        server.start();
        System.out.println("HTTPS 服务器已启动: https://localhost:8443");
    }
}
```

## WebSocket 服务器

完整的 WebSocket 服务器示例。

```java
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.websocket.coder.*;
import cc.jfire.jnet.extend.websocket.dto.WebSocketFrame;
import cc.jfire.jnet.server.AioServer;
import java.nio.charset.StandardCharsets;

public class WebSocketServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // HTTP 升级到 WebSocket
            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());
            pipeline.addReadProcessor(new WebSocketUpgradeDecoder());

            // WebSocket 帧处理
            pipeline.addReadProcessor(new WebSocketFrameDecoder(true)); // 服务端模式

            // 业务处理器
            pipeline.addReadProcessor(new ReadProcessor<WebSocketFrame>() {
                @Override
                public void read(WebSocketFrame frame, ReadProcessorNode next) {
                    if (frame.getOpcode() == WebSocketFrame.OPCODE_TEXT) {
                        // 处理文本消息
                        String message = StandardCharsets.UTF_8
                            .decode(frame.getPayload().readableByteBuffer())
                            .toString();

                        System.out.println("收到消息: " + message);

                        // 回复消息
                        String response = "Echo: " + message;
                        WebSocketFrame responseFrame = new WebSocketFrame();
                        responseFrame.setFin(true);
                        responseFrame.setOpcode(WebSocketFrame.OPCODE_TEXT);
                        responseFrame.setPayloadText(response);

                        pipeline.fireWrite(responseFrame);
                    } else if (frame.getOpcode() == WebSocketFrame.OPCODE_CLOSE) {
                        System.out.println("客户端关闭连接");
                        pipeline.shutdownInput();
                    }

                    frame.free();
                }
            });

            // WebSocket 编码器
            pipeline.addWriteProcessor(new WebSocketFrameEncoder(false)); // 服务端模式
            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
        });

        server.start();
        System.out.println("WebSocket 服务器已启动: ws://localhost:8080");
    }
}
```

## 自定义协议服务器

使用自定义解码器的服务器。

```java
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;
import java.nio.charset.StandardCharsets;

public class CustomProtocolServer {
    // 自定义协议：[4字节长度][消息体]
    static class LengthFieldDecoder extends AbstractDecoder {
        @Override
        protected void process0(ReadProcessorNode next) {
            while (true) {
                if (accumulation.remainRead() < 4) {
                    return; // 数据不足
                }

                int length = accumulation.getInt(accumulation.getReadPosi());

                if (accumulation.remainRead() < 4 + length) {
                    return; // 数据不足
                }

                accumulation.skip(4);
                IoBuffer message = accumulation.slice(length);
                next.fireRead(message);
            }
        }
    }

    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 添加自定义解码器
            pipeline.addReadProcessor(new LengthFieldDecoder());

            // 业务处理器
            pipeline.addReadProcessor((IoBuffer data, next) -> {
                String message = StandardCharsets.UTF_8
                    .decode(data.readableByteBuffer())
                    .toString();

                System.out.println("收到消息: " + message);

                // 回复消息
                String response = "Received: " + message;
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                IoBuffer responseBuffer = pipeline.allocator().allocate(4 + responseBytes.length);
                responseBuffer.putInt(responseBytes.length);
                responseBuffer.put(responseBytes);

                pipeline.fireWrite(responseBuffer);
                data.free();
            });
        });

        server.start();
        System.out.println("自定义协议服务器已启动，监听端口 8080");
    }
}
```

## 使用连接附件

在连接上存储状态信息。

```java
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.server.AioServer;

public class StatefulServer {
    static class SessionData {
        int requestCount = 0;
        String userId;
    }

    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 初始化连接附件
            pipeline.setAttach(new SessionData());

            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());

            pipeline.addReadProcessor(new ReadProcessor<HttpRequest>() {
                @Override
                public void read(HttpRequest request, ReadProcessorNode next) {
                    Pipeline p = next.pipeline();

                    // 获取连接附件
                    SessionData session = (SessionData) p.getAttach();
                    session.requestCount++;

                    // 创建响应
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", "text/plain");
                    response.setBodyText("请求次数: " + session.requestCount);

                    p.fireWrite(response);
                    request.close();
                }
            });

            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
        });

        server.start();
        System.out.println("有状态服务器已启动: http://localhost:8080");
    }
}
```

## 处理器链组合

展示不同处理器的组合方式。

```java
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.websocket.coder.*;
import cc.jfire.jnet.server.AioServer;

public class ProcessorChainExample {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 读处理器链（入站）
            // 1. 协议层：SSL 解密（如果需要）
            // pipeline.addReadProcessor(new SSLDecoder(sslEngine));

            // 2. 应用层：HTTP 解码
            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());

            // 3. 功能层：OPTIONS 处理
            pipeline.addReadProcessor(new OptionsProcessor());

            // 4. 协议升级：WebSocket 升级
            pipeline.addReadProcessor(new WebSocketUpgradeDecoder());
            pipeline.addReadProcessor(new WebSocketFrameDecoder(true));

            // 5. 业务层：业务逻辑处理
            pipeline.addReadProcessor((Object data, next) -> {
                // 处理 HttpRequest 或 WebSocketFrame
                System.out.println("收到数据: " + data.getClass().getSimpleName());
                next.fireRead(data);
            });

            // 写处理器链（出站）
            // 1. 应用层：HTTP/WebSocket 编码
            pipeline.addWriteProcessor(new WebSocketFrameEncoder(false));
            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));

            // 2. 协议层：SSL 加密（如果需要）
            // pipeline.addWriteProcessor(new SSLEncoder(sslEngine));
        });

        server.start();
        System.out.println("服务器已启动，支持 HTTP 和 WebSocket");
    }
}
```

## 错误处理示例

```java
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.server.AioServer;

public class ErrorHandlingServer {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            pipeline.addReadProcessor(new HttpRequestPartDecoder());
            pipeline.addReadProcessor(new HttpRequestAggregator());

            pipeline.addReadProcessor(new ReadProcessor<HttpRequest>() {
                @Override
                public void read(HttpRequest request, ReadProcessorNode next) {
                    try {
                        // 可能抛出异常的业务逻辑
                        processRequest(request, pipeline);
                    } catch (Exception e) {
                        // 捕获异常并返回错误响应
                        System.err.println("处理请求失败: " + e.getMessage());

                        HttpResponse errorResponse = new HttpResponse();
                        errorResponse.setStatusCode(500);
                        errorResponse.addHeader("Content-Type", "text/plain");
                        errorResponse.setBodyText("Internal Server Error");

                        pipeline.fireWrite(errorResponse);
                    } finally {
                        request.close();
                    }
                }

                @Override
                public void readFailed(Throwable e, ReadProcessorNode next) {
                    System.err.println("读取失败: " + e.getMessage());
                    next.pipeline().shutdownInput();
                }

                private void processRequest(HttpRequest request, Pipeline pipeline) {
                    HttpResponse response = new HttpResponse();
                    response.addHeader("Content-Type", "text/plain");
                    response.setBodyText("Success");
                    pipeline.fireWrite(response);
                }
            });

            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
        });

        server.start();
        System.out.println("服务器已启动，支持错误处理");
    }
}
```
