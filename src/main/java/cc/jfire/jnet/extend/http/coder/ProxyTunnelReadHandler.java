package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 代理隧道读处理器
 * 在隧道建立前拦截并解析 CONNECT 响应，建立后透传数据给 SSL 解码器
 */
public class ProxyTunnelReadHandler implements ReadProcessor<IoBuffer>
{
    private final CountDownLatch             tunnelLatch;
    private final AtomicBoolean              tunnelEstablished;
    private final AtomicReference<Throwable> errorRef;

    private volatile boolean  tunnelReady = false;
    private          IoBuffer accumulation;

    public ProxyTunnelReadHandler(CountDownLatch tunnelLatch,
                                  AtomicBoolean tunnelEstablished,
                                  AtomicReference<Throwable> errorRef)
    {
        this.tunnelLatch       = tunnelLatch;
        this.tunnelEstablished = tunnelEstablished;
        this.errorRef          = errorRef;
    }

    @Override
    public void read(IoBuffer data, ReadProcessorNode next)
    {
        if (tunnelReady)
        {
            // 隧道已建立，透传数据给 SSL 解码器
            next.fireRead(data);
            return;
        }

        // 累积数据
        if (accumulation == null)
        {
            accumulation = data;
        }
        else
        {
            accumulation.put(data);
            data.free();
        }

        // 尝试解析 CONNECT 响应
        if (parseConnectResponse())
        {
            tunnelReady = true;
            tunnelEstablished.set(true);
            tunnelLatch.countDown();

            // 如果有剩余数据，传递给 SSL 解码器
            if (accumulation != null && accumulation.remainRead() > 0)
            {
                next.fireRead(accumulation);
                accumulation = null;
            }
            else if (accumulation != null)
            {
                accumulation.free();
                accumulation = null;
            }
        }
    }

    /**
     * 解析 CONNECT 响应
     * 期望格式: HTTP/1.1 200 Connection Established\r\n...\r\n\r\n
     */
    private boolean parseConnectResponse()
    {
        if (accumulation == null || accumulation.remainRead() < 4)
        {
            return false;
        }

        // 查找 \r\n\r\n 标记响应头结束
        int readPosi  = accumulation.getReadPosi();
        int writePosi = accumulation.getWritePosi();

        for (int i = readPosi; i < writePosi - 3; i++)
        {
            if (accumulation.get(i) == '\r' &&
                accumulation.get(i + 1) == '\n' &&
                accumulation.get(i + 2) == '\r' &&
                accumulation.get(i + 3) == '\n')
            {
                // 找到响应头结束位置
                int headerEndPosi = i + 4;
                int headerLength  = headerEndPosi - readPosi;

                // 解析状态行
                byte[] headerBytes = new byte[headerLength];
                for (int j = 0; j < headerLength; j++)
                {
                    headerBytes[j] = accumulation.get(readPosi + j);
                }
                String headerStr = new String(headerBytes, StandardCharsets.US_ASCII);

                // 检查状态码是否为 200
                if (headerStr.startsWith("HTTP/") && headerStr.contains(" 200 "))
                {
                    // 移动读取位置
                    accumulation.setReadPosi(headerEndPosi);
                    return true;
                }
                else
                {
                    // 代理拒绝连接
                    String statusLine = headerStr.split("\r\n")[0];
                    errorRef.set(new RuntimeException("代理服务器返回非 200 响应: " + statusLine));
                    tunnelLatch.countDown();
                    return false;
                }
            }
        }

        return false;
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (!tunnelReady)
        {
            errorRef.set(e);
            tunnelLatch.countDown();
        }
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        next.fireReadFailed(e);
    }
}
