import cc.jfire.jnet.extend.http.client.HttpClient;
import cc.jfire.jnet.extend.http.client.StreamableResponseFuture;
import cc.jfire.jnet.extend.http.dto.HttpRequest;
import cc.jfire.jnet.extend.http.dto.HttpResponsePart;
import cc.jfire.jnet.extend.http.dto.HttpResponsePartHead;
import cc.jfire.jnet.extend.http.dto.HttpResponseChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpResponseFixLengthBodyPart;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import java.nio.charset.StandardCharsets;

/**
 * 流式 HTTP 客户端模板
 * 演示如何使用 JNet HttpClient 处理流式响应（如 SSE、分块传输等）
 */
public class StreamingHttpClient {

    private final HttpClient client;

    public StreamingHttpClient() {
        this.client = new HttpClient();
    }

    /**
     * 发起流式请求
     *
     * @param url 请求 URL
     * @param onChunk 处理每个数据块的回调
     * @param onComplete 完成时的回调
     * @param onError 错误处理回调
     */
    public void streamRequest(String url,
                              ChunkHandler onChunk,
                              Runnable onComplete,
                              ErrorHandler onError) throws Exception {

        try (HttpRequest request = new HttpRequest()
                .setUrl(url)
                .get()
                .addHeader("Accept", "text/event-stream")) {

            StreamableResponseFuture future = client.streamCall(
                request,
                // 处理每个响应片段
                part -> {
                    try {
                        if (part instanceof HttpResponsePartHead) {
                            // 响应头部
                            HttpResponsePartHead head = (HttpResponsePartHead) part;
                            System.out.println("状态码: " + head.getStatusCode());
                            System.out.println("响应头: " + head.getHeaders());

                        } else if (part instanceof HttpResponseChunkedBodyPart) {
                            // 分块响应体
                            HttpResponseChunkedBodyPart bodyPart = (HttpResponseChunkedBodyPart) part;
                            IoBuffer chunk = bodyPart.getChunk();

                            if (chunk != null && chunk.remainRead() > 0) {
                                String chunkText = StandardCharsets.UTF_8
                                    .decode(chunk.readableByteBuffer())
                                    .toString();

                                if (onChunk != null) {
                                    onChunk.handle(chunkText);
                                }
                            }

                        } else if (part instanceof HttpResponseFixLengthBodyPart) {
                            // 固定长度响应体
                            HttpResponseFixLengthBodyPart bodyPart = (HttpResponseFixLengthBodyPart) part;
                            IoBuffer body = bodyPart.getBody();

                            if (body != null && body.remainRead() > 0) {
                                String bodyText = StandardCharsets.UTF_8
                                    .decode(body.readableByteBuffer())
                                    .toString();

                                if (onChunk != null) {
                                    onChunk.handle(bodyText);
                                }
                            }
                        }

                        // 检查是否是最后一个片段
                        if (part.isLast()) {
                            System.out.println("流式响应完成");
                            if (onComplete != null) {
                                onComplete.run();
                            }
                        }

                    } finally {
                        // 释放资源
                        part.free();
                    }
                },
                // 错误处理
                error -> {
                    System.err.println("请求失败: " + error.getMessage());
                    if (onError != null) {
                        onError.handle(error);
                    }
                }
            );
        }
    }

    /**
     * 数据块处理器接口
     */
    @FunctionalInterface
    public interface ChunkHandler {
        void handle(String chunk);
    }

    /**
     * 错误处理器接口
     */
    @FunctionalInterface
    public interface ErrorHandler {
        void handle(Throwable error);
    }

    public static void main(String[] args) throws Exception {
        StreamingHttpClient client = new StreamingHttpClient();

        // 示例 1: 处理 SSE (Server-Sent Events) 流
        System.out.println("=== SSE 流式请求示例 ===");
        client.streamRequest(
            "https://api.example.com/stream/events",
            // 处理每个数据块
            chunk -> {
                System.out.print(chunk);
            },
            // 完成回调
            () -> {
                System.out.println("\n流式请求完成");
            },
            // 错误回调
            error -> {
                error.printStackTrace();
            }
        );

        // 示例 2: 处理大文件下载流
        System.out.println("\n=== 大文件流式下载示例 ===");
        final long[] totalBytes = {0};

        client.streamRequest(
            "https://example.com/large-file.zip",
            // 处理每个数据块
            chunk -> {
                totalBytes[0] += chunk.getBytes(StandardCharsets.UTF_8).length;
                System.out.println("已接收: " + totalBytes[0] + " 字节");
            },
            // 完成回调
            () -> {
                System.out.println("下载完成，总计: " + totalBytes[0] + " 字节");
            },
            // 错误回调
            error -> {
                System.err.println("下载失败: " + error.getMessage());
            }
        );

        // 示例 3: 处理 ChatGPT 风格的流式响应
        System.out.println("\n=== AI 流式响应示例 ===");
        StringBuilder fullResponse = new StringBuilder();

        client.streamRequest(
            "https://api.example.com/ai/chat/stream",
            // 处理每个数据块
            chunk -> {
                // 实时打印每个 token
                System.out.print(chunk);
                fullResponse.append(chunk);
            },
            // 完成回调
            () -> {
                System.out.println("\n\n完整响应: " + fullResponse.toString());
            },
            // 错误回调
            error -> {
                System.err.println("AI 请求失败: " + error.getMessage());
            }
        );
    }
}
