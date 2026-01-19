# 编写自定义 ReadProcessor 指南

## 基础概念

ReadProcessor 是 JNet 管道架构的核心组件，用于处理入站数据。每个 ReadProcessor 负责特定的数据处理任务，并通过 `next.fireRead()` 将数据传递给下一个处理器。

## ReadProcessor 接口

```java
public interface ReadProcessor<T> {
    void read(T data, ReadProcessorNode next);
    default void readFailed(Throwable e, ReadProcessorNode next);
    default void readCompleted(ReadProcessorNode next);
    default void pipelineComplete(Pipeline pipeline, ReadProcessorNode next);
}
```

## 编写自定义 ReadProcessor 的步骤

### 1. 简单的透传处理器

最简单的 ReadProcessor 只是处理数据并传递给下一个处理器。

```java
public class SimpleProcessor implements ReadProcessor<String> {
    @Override
    public void read(String data, ReadProcessorNode next) {
        // 处理数据
        System.out.println("收到数据: " + data);

        // 传递给下一个处理器
        next.fireRead(data);
    }
}
```

### 2. 数据转换处理器

将一种类型的数据转换为另一种类型。

```java
public class StringToUpperCaseProcessor implements ReadProcessor<String> {
    @Override
    public void read(String data, ReadProcessorNode next) {
        // 转换数据
        String upperCase = data.toUpperCase();

        // 传递转换后的数据
        next.fireRead(upperCase);
    }
}
```

### 3. 数据过滤处理器

根据条件过滤数据，只传递符合条件的数据。

```java
public class FilterProcessor implements ReadProcessor<String> {
    @Override
    public void read(String data, ReadProcessorNode next) {
        // 过滤逻辑
        if (data.startsWith("VALID:")) {
            // 只传递有效数据
            next.fireRead(data);
        } else {
            // 无效数据，不传递
            System.out.println("过滤掉无效数据: " + data);
        }
    }
}
```

### 4. 状态累积处理器

累积多个数据片段，达到条件后一次性传递。

```java
public class AccumulatorProcessor implements ReadProcessor<String> {
    private final StringBuilder accumulator = new StringBuilder();
    private final int threshold = 100;

    @Override
    public void read(String data, ReadProcessorNode next) {
        // 累积数据
        accumulator.append(data);

        // 达到阈值时传递
        if (accumulator.length() >= threshold) {
            next.fireRead(accumulator.toString());
            accumulator.setLength(0); // 清空累积器
        }
    }
}
```

## 编写自定义解码器

解码器是特殊的 ReadProcessor，用于将字节流解析为应用层对象。JNet 提供了 `AbstractDecoder` 基类简化解码器开发。

### AbstractDecoder 基类

```java
public abstract class AbstractDecoder implements ReadProcessor<IoBuffer> {
    protected IoBuffer accumulation; // 累积缓冲区

    protected abstract void process0(ReadProcessorNode next);
}
```

**关键特性：**
- 自动管理累积缓冲区 `accumulation`
- 自动处理数据累积和释放
- 子类只需实现 `process0()` 方法

### 示例 1：固定长度消息解码器

解析固定长度的消息（如每条消息 10 字节）。

```java
public class FixedLengthMessageDecoder extends AbstractDecoder {
    private static final int MESSAGE_LENGTH = 10;

    @Override
    protected void process0(ReadProcessorNode next) {
        // 循环解析，直到数据不足
        while (accumulation.remainRead() >= MESSAGE_LENGTH) {
            // 切片出一条消息
            IoBuffer message = accumulation.slice(MESSAGE_LENGTH);

            // 传递给下一个处理器
            next.fireRead(message);
        }

        // 数据不足时，等待更多数据
        // accumulation 会自动保留未处理的数据
    }
}
```

### 示例 2：基于长度字段的消息解码器

消息格式：`[4字节长度][消息体]`

```java
public class LengthFieldMessageDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        while (true) {
            // 检查是否有足够的数据读取长度字段
            if (accumulation.remainRead() < 4) {
                return; // 数据不足，等待
            }

            // 读取长度字段（不移动读指针）
            int length = accumulation.getInt(accumulation.getReadPosi());

            // 检查是否有足够的数据读取完整消息
            if (accumulation.remainRead() < 4 + length) {
                return; // 数据不足，等待
            }

            // 跳过长度字段
            accumulation.skip(4);

            // 切片出消息体
            IoBuffer message = accumulation.slice(length);

            // 传递给下一个处理器
            next.fireRead(message);
        }
    }
}
```

### 示例 3：分隔符消息解码器

消息以 `\n` 分隔。

```java
public class LineBasedMessageDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        while (true) {
            // 查找分隔符
            int delimiterIndex = findDelimiter();

            if (delimiterIndex == -1) {
                return; // 未找到分隔符，等待更多数据
            }

            // 计算消息长度（不包括分隔符）
            int messageLength = delimiterIndex - accumulation.getReadPosi();

            // 切片出消息
            IoBuffer message = accumulation.slice(messageLength);

            // 跳过分隔符
            accumulation.skip(1);

            // 传递给下一个处理器
            next.fireRead(message);
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
```

### 示例 4：状态机解码器

复杂协议通常需要状态机来解析。

```java
public class StateMachineDecoder extends AbstractDecoder {
    private enum State {
        HEADER,
        BODY
    }

    private State state = State.HEADER;
    private int bodyLength = 0;

    @Override
    protected void process0(ReadProcessorNode next) {
        boolean continueProcessing;
        do {
            continueProcessing = switch (state) {
                case HEADER -> parseHeader();
                case BODY -> parseBody(next);
            };
        } while (continueProcessing);
    }

    private boolean parseHeader() {
        if (accumulation.remainRead() < 4) {
            return false; // 数据不足
        }

        bodyLength = accumulation.getInt();
        state = State.BODY;
        return true; // 继续处理
    }

    private boolean parseBody(ReadProcessorNode next) {
        if (accumulation.remainRead() < bodyLength) {
            return false; // 数据不足
        }

        IoBuffer body = accumulation.slice(bodyLength);
        next.fireRead(body);

        // 重置状态
        state = State.HEADER;
        bodyLength = 0;
        return true; // 继续处理
    }
}
```

## 高级技巧

### 1. 使用 Pipeline 存储状态

如果需要在处理器之间共享状态，可以使用 Pipeline 的持久化存储。

```java
public class StatefulProcessor implements ReadProcessor<String> {
    private static final String KEY = "my-state";

    @Override
    public void read(String data, ReadProcessorNode next) {
        Pipeline pipeline = next.pipeline();

        // 读取状态
        Integer count = (Integer) pipeline.getPersistenceStore(KEY);
        if (count == null) {
            count = 0;
        }

        // 更新状态
        count++;
        pipeline.putPersistenceStore(KEY, count);

        System.out.println("处理第 " + count + " 条消息");
        next.fireRead(data);
    }
}
```

### 2. 处理多种数据类型

使用 `Object` 作为泛型参数，并使用 `instanceof` 判断类型。

```java
public class MultiTypeProcessor implements ReadProcessor<Object> {
    @Override
    public void read(Object data, ReadProcessorNode next) {
        if (data instanceof String str) {
            // 处理字符串
            System.out.println("字符串: " + str);
        } else if (data instanceof IoBuffer buffer) {
            // 处理 IoBuffer
            System.out.println("IoBuffer: " + buffer.remainRead() + " 字节");
        }

        next.fireRead(data);
    }
}
```

### 3. 错误处理

重写 `readFailed()` 方法处理错误。

```java
public class ErrorHandlingProcessor implements ReadProcessor<String> {
    @Override
    public void read(String data, ReadProcessorNode next) {
        try {
            // 可能抛出异常的处理逻辑
            processData(data);
            next.fireRead(data);
        } catch (Exception e) {
            // 传递错误
            next.fireReadFailed(e);
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next) {
        // 自定义错误处理
        System.err.println("处理失败: " + e.getMessage());

        // 继续传递错误
        next.fireReadFailed(e);
    }

    private void processData(String data) throws Exception {
        // 处理逻辑
    }
}
```

### 4. 资源清理

在解码器中，必须正确释放 IoBuffer 资源。

```java
public class ResourceCleanupDecoder extends AbstractDecoder {
    @Override
    protected void process0(ReadProcessorNode next) {
        try {
            // 解析逻辑
            if (accumulation.remainRead() < 4) {
                return;
            }

            IoBuffer message = accumulation.slice(4);
            next.fireRead(message);

        } catch (Exception e) {
            // 发生异常时释放累积缓冲区
            if (accumulation != null) {
                accumulation.free();
                accumulation = null;
            }
            next.pipeline().shutdownInput();
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next) {
        // 释放资源
        if (accumulation != null) {
            accumulation.free();
            accumulation = null;
        }
        next.fireReadFailed(e);
    }
}
```

## 最佳实践

1. **单一职责：** 每个处理器只负责一个特定任务
2. **数据传递：** 始终调用 `next.fireRead()` 传递数据，除非需要过滤
3. **资源管理：** 使用完 IoBuffer 后必须调用 `free()` 释放
4. **错误处理：** 捕获异常并调用 `next.fireReadFailed()` 或 `pipeline.shutdownInput()`
5. **状态重置：** 解析完一条消息后，重置解码器状态
6. **循环解析：** 在 `process0()` 中使用循环，尽可能多地解析数据
7. **数据不足：** 数据不足时直接返回，等待更多数据
8. **避免阻塞：** 不要在处理器中执行阻塞操作

## 完整示例：自定义 JSON 消息解码器

```java
public class JsonMessageDecoder extends AbstractDecoder {
    private static final byte OPEN_BRACE = '{';
    private static final byte CLOSE_BRACE = '}';

    @Override
    protected void process0(ReadProcessorNode next) {
        while (true) {
            // 查找 JSON 对象的起始位置
            int startIndex = findJsonStart();
            if (startIndex == -1) {
                return; // 未找到起始标记
            }

            // 跳过起始位置之前的数据
            if (startIndex > accumulation.getReadPosi()) {
                accumulation.setReadPosi(startIndex);
            }

            // 查找 JSON 对象的结束位置
            int endIndex = findJsonEnd();
            if (endIndex == -1) {
                return; // 未找到结束标记，等待更多数据
            }

            // 计算 JSON 长度
            int jsonLength = endIndex - accumulation.getReadPosi() + 1;

            // 切片出 JSON 数据
            IoBuffer jsonBuffer = accumulation.slice(jsonLength);

            // 解析 JSON 字符串
            String jsonString = StandardCharsets.UTF_8
                .decode(jsonBuffer.readableByteBuffer())
                .toString();

            // 释放 buffer
            jsonBuffer.free();

            // 传递给下一个处理器
            next.fireRead(jsonString);
        }
    }

    private int findJsonStart() {
        for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++) {
            if (accumulation.get(i) == OPEN_BRACE) {
                return i;
            }
        }
        return -1;
    }

    private int findJsonEnd() {
        int braceCount = 0;
        for (int i = accumulation.getReadPosi(); i < accumulation.getWritePosi(); i++) {
            byte b = accumulation.get(i);
            if (b == OPEN_BRACE) {
                braceCount++;
            } else if (b == CLOSE_BRACE) {
                braceCount--;
                if (braceCount == 0) {
                    return i;
                }
            }
        }
        return -1;
    }
}
```

## 测试自定义处理器

```java
public class ProcessorTest {
    public static void main(String[] args) {
        ChannelConfig config = new ChannelConfig().setPort(8080);

        AioServer server = AioServer.newAioServer(config, pipeline -> {
            // 添加自定义处理器
            pipeline.addReadProcessor(new JsonMessageDecoder());
            pipeline.addReadProcessor(new ReadProcessor<String>() {
                @Override
                public void read(String data, ReadProcessorNode next) {
                    System.out.println("收到 JSON: " + data);
                    // 处理 JSON 数据
                }
            });
        });

        server.start();
    }
}
```
