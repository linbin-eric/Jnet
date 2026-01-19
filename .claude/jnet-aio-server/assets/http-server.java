import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.extend.http.coder.*;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponse;
import cc.jfire.jnet.server.AioServer;

/**
 * HTTP 服务器模板
 * 演示如何创建一个完整的 HTTP 服务器
 */
public class HttpServer {

    public static void main(String[] args) {
        // 配置服务器端口
        ChannelConfig config = new ChannelConfig()
            .setPort(8080);

        // 创建 AIO 服务器
        AioServer server = AioServer.newAioServer(config, pipeline -> {

            // 1. 添加 HTTP 请求解码器
            pipeline.addReadProcessor(new HttpRequestPartDecoder());

            // 2. 添加 HTTP 请求聚合器
            pipeline.addReadProcessor(new HttpRequestAggregator());

            // 3. 添加 OPTIONS 请求处理器（CORS 支持）
            pipeline.addReadProcessor(new OptionsProcessor());

            // 4. 添加业务处理器
            pipeline.addReadProcessor(new ReadProcessor<HttpRequest>() {
                @Override
                public void read(HttpRequest request, ReadProcessorNode next) {
                    try {
                        // 打印请求信息
                        System.out.println("收到请求: " + request.getHead().getMethod()
                            + " " + request.getHead().getPath());

                        // 路由处理
                        String path = request.getHead().getPath();
                        HttpResponse response = new HttpResponse();

                        if (path.equals("/")) {
                            // 首页
                            response.setStatusCode(200);
                            response.addHeader("Content-Type", "text/html; charset=utf-8");
                            response.setBodyText("<h1>欢迎使用 JNet HTTP 服务器</h1>");

                        } else if (path.equals("/api/hello")) {
                            // API 接口
                            response.setStatusCode(200);
                            response.addHeader("Content-Type", "application/json; charset=utf-8");
                            response.setBodyText("{\"message\":\"Hello from JNet!\"}");

                        } else if (path.startsWith("/api/echo")) {
                            // Echo 接口
                            String body = request.getStrBody();
                            response.setStatusCode(200);
                            response.addHeader("Content-Type", "text/plain; charset=utf-8");
                            response.setBodyText("Echo: " + (body != null ? body : ""));

                        } else {
                            // 404 Not Found
                            response.setStatusCode(404);
                            response.addHeader("Content-Type", "text/html; charset=utf-8");
                            response.setBodyText("<h1>404 Not Found</h1>");
                        }

                        // 发送响应
                        pipeline.fireWrite(response);

                    } catch (Exception e) {
                        // 错误处理
                        System.err.println("处理请求失败: " + e.getMessage());
                        e.printStackTrace();

                        HttpResponse errorResponse = new HttpResponse();
                        errorResponse.setStatusCode(500);
                        errorResponse.addHeader("Content-Type", "text/plain");
                        errorResponse.setBodyText("Internal Server Error");
                        pipeline.fireWrite(errorResponse);

                    } finally {
                        // 释放请求资源
                        request.close();
                    }
                }

                @Override
                public void readFailed(Throwable e, ReadProcessorNode next) {
                    System.err.println("读取失败: " + e.getMessage());
                    next.pipeline().shutdownInput();
                }
            });

            // 5. 添加 HTTP 响应编码器
            pipeline.addWriteProcessor(new HttpRespEncoder(pipeline.allocator()));
        });

        // 启动服务器
        server.start();
        System.out.println("HTTP 服务器已启动: http://localhost:8080");
        System.out.println("可用路由:");
        System.out.println("  GET  /              - 首页");
        System.out.println("  GET  /api/hello     - Hello API");
        System.out.println("  POST /api/echo      - Echo API");
    }
}
