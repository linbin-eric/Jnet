import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.server.AioServer;
import java.nio.charset.StandardCharsets;

/**
 * 自定义解码器模板
 * 演示如何编写基于长度字段的自定义协议解码器
 *
 * 协议格式：[4字节长度][消息体]
 */
public class CustomDecoder {

    /**
     * 自定义长度字段解码器
     * 消息格式：[4字节长度(int)][消息体(UTF-8字符串)]
     */
    static class LengthFieldMessageDecoder extends AbstractDecoder {

        @Override
        protected void process0(ReadProcessorNode next) {
            // 循环解析，尽可能多地处理数据
            while (true) {
                // 1. 检查是否有足够的数据读取长度字段
                if (accumulation.remainRead() < 4) {
                    return; // 数据不足，等待更多数据
                }

                // 2. 读取长度字段（不移动读指针）
                int messageLength = accumulation.getInt(accumulation.getReadPosi());

                // 3. 验证长度字段的合法性
                if (messageLength < 0 || messageLength > 1024 * 1024) {
                    // 长度非法，关闭连接
                    System.err.println("非法的消息长度: " + messageLength);
                    next.pipeline().shutdownInput();
                    return;
                }

                // 4. 检查是否有足够的数据读取完整消息
                if (accumulation.remainRead() < 4 + messageLength) {
                    return; // 数据不足，等待更多数据
                }

                // 5. 跳过长度字段
                accumulation.skip(4);

                // 6. 切片出消息体
                IoBuffer messageBuffer = accumulation.slice(messageLength);

                // 7. 传递给下一个处理器
                next.fireRead(messageBuffer);

                // 8. 继续循环，处理剩余数据
            }
        }
    }

    /**
     * 分隔符解码器示例
     * 消息以 \n 分隔
     */
    static class LineBasedDecoder extends AbstractDecoder {

        @Override
        protected void process0(ReadProcessorNode next) {
            while (true) {
                // 查找换行符
                int delimiterIndex = findDelimiter();

                if (delimiterIndex == -1) {
                    return; // 未找到分隔符，等待更多数据
                }

                // 计算消息长度（不包括分隔符）
                int messageLength = delimiterIndex - accumulation.getReadPosi();

                // 切片出消息
                IoBuffer messageBuffer = accumulation.slice(messageLength);

                // 跳过分隔符
                accumulation.skip(1);

                // 传递给下一个处理器
                next.fireRead(messageBuffer);
            }
        }

        private int findDelimiter() {
            for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++) {
                if (accumulation.get(i) == '\n') {
                    return i;
                }
            }
            return -1;
        }
    }

    /**
     * 状态机解码器示例
     * 演示如何使用状态机解析复杂协议
     */
    static class StateMachineDecoder extends AbstractDecoder {

        private enum State {
            HEADER,  // 解析头部
            BODY     // 解析消息体
        }

        private State state = State.HEADER;
        private int bodyLength = 0;

        @Override
        protected void process0(ReadProcessorNode next) {
            boolean continueProcessing;
            do {
                // 根据当前状态执行相应的解析逻辑
                continueProcessing = switch (state) {
                    case HEADER -> parseHeader();
                    case BODY -> parseBody(next);
                };
            } while (continueProcessing);
        }

        private boolean parseHeader() {
            // 检查是否有足够的数据读取头部（4字节）
            if (accumulation.remainRead() < 4) {
                return false; // 数据不足
            }

            // 读取消息体长度
            bodyLength = accumulation.getInt();

            // 切换到 BODY 状态
            state = State.BODY;
            return true; // 继续处理
        }

        private boolean parseBody(ReadProcessorNode next) {
            // 检查是否有足够的数据读取消息体
            if (accumulation.remainRead() < bodyLength) {
                return false; // 数据不足
            }

            // 切片出消息体
            IoBuffer bodyBuffer = accumulation.slice(bodyLength);

            // 传递给下一个处理器
            next.fireRead(bodyBuffer);

            // 重置状态，准备解析下一条消息
            state = State.HEADER;
            bodyLength = 0;

            return true; // 继续处理
        }
    }

    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {

            // 选择一个解码器
            // 1. 基于长度字段的解码器
            pipeline.addReadProcessor(new LengthFieldMessageDecoder());

            // 2. 基于分隔符的解码器
            // pipeline.addReadProcessor(new LineBasedDecoder());

            // 3. 状态机解码器
            // pipeline.addReadProcessor(new StateMachineDecoder());

            // 添加业务处理器
            pipeline.addReadProcessor((IoBuffer data, next) -> {
                try {
                    // 将 IoBuffer 转换为字符串
                    String message = StandardCharsets.UTF_8
                        .decode(data.readableByteBuffer())
                        .toString();

                    System.out.println("收到消息: " + message);

                    // 回复消息
                    String response = "Received: " + message;
                    byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                    // 构造响应（带长度字段）
                    IoBuffer responseBuffer = pipeline.allocator()
                        .allocate(4 + responseBytes.length);
                    responseBuffer.putInt(responseBytes.length);
                    responseBuffer.put(responseBytes);

                    // 发送响应
                    pipeline.fireWrite(responseBuffer);

                } finally {
                    // 释放资源
                    data.free();
                }
            });
        });

        server.start();
        System.out.println("自定义协议服务器已启动，监听端口: 8080");
        System.out.println("协议格式: [4字节长度][消息体]");
    }
}
