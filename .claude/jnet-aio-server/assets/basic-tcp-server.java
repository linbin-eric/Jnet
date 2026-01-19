import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;
import java.nio.charset.StandardCharsets;

/**
 * 基础 TCP 服务器模板
 * 演示如何创建一个简单的 TCP Echo 服务器
 */
public class BasicTcpServer {

    public static void main(String[] args) {
        // 配置服务器端口
        ChannelConfig config = new ChannelConfig()
            .setPort(8080);

        // 创建 AIO 服务器
        AioServer server = AioServer.newAioServer(config, pipeline -> {

            // 添加读处理器 - 处理接收到的数据
            pipeline.addReadProcessor(new ReadProcessor<IoBuffer>() {
                @Override
                public void read(IoBuffer data, ReadProcessorNode next) {
                    try {
                        // 读取接收到的数据
                        String receivedText = StandardCharsets.UTF_8
                            .decode(data.readableByteBuffer())
                            .toString();

                        System.out.println("收到数据: " + receivedText);

                        // 创建响应数据
                        String response = "Echo: " + receivedText;
                        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                        // 分配缓冲区并写入响应
                        IoBuffer responseBuffer = pipeline.allocator().allocate(responseBytes.length);
                        responseBuffer.put(responseBytes);

                        // 发送响应
                        pipeline.fireWrite(responseBuffer);

                    } finally {
                        // 释放接收到的数据缓冲区
                        data.free();
                    }
                }

                @Override
                public void readFailed(Throwable e, ReadProcessorNode next) {
                    System.err.println("读取失败: " + e.getMessage());
                    next.pipeline().shutdownInput();
                }
            });
        });

        // 启动服务器
        server.start();
        System.out.println("TCP 服务器已启动，监听端口: 8080");
        System.out.println("使用 telnet localhost 8080 进行测试");
    }
}
