package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 代理隧道读处理器
 * 在隧道建立前拦截并解析 CONNECT 响应，建立后透传数据给 SSL 解码器
 */
public class ProxyTunnelReadHandler implements ReadProcessor<IoBuffer>
{
    public static final String         KEY               = "proxyTunnelReadHandler";
    private final       String         domain;
    private final       int            port;
    private final       CountDownLatch tunnelLatch       = new CountDownLatch(1);
    private volatile    boolean        tunnelEstablished = false;
    private volatile    Throwable      tunnelError       = null;
    private volatile    boolean        tunnelReady       = false;
    private             IoBuffer       accumulation;

    public ProxyTunnelReadHandler(String domain, int port)
    {
        this.domain = domain;
        this.port   = port;
    }

    @Override
    public void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        // 发送 CONNECT 请求
        String   connectRequestStr = "CONNECT " + domain + ":" + port + " HTTP/1.1\r\n" + "Host: " + domain + ":" + port + "\r\n\r\n";
        IoBuffer connectBuffer     = pipeline.allocator().allocate(connectRequestStr.length());
        connectBuffer.put(connectRequestStr.getBytes(StandardCharsets.US_ASCII));
        pipeline.directWrite(connectBuffer);
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
            tunnelReady       = true;
            tunnelEstablished = true;
            tunnelLatch.countDown();
            // 隧道建立成功，传递 pipelineComplete 事件给后续处理器
            next.firePipelineComplete(next.pipeline());
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
            if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n' && accumulation.get(i + 2) == '\r' && accumulation.get(i + 3) == '\n')
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
                    tunnelError = new RuntimeException("代理服务器返回非 200 响应: " + statusLine);
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
            tunnelError = e;
            tunnelLatch.countDown();
        }
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        next.fireReadFailed(e);
    }

    /**
     * 等待隧道建立完成
     *
     * @param timeout 超时时间
     * @param unit    时间单位
     * @return true 表示在超时前隧道建立完成，false 表示超时
     * @throws InterruptedException 如果等待被中断
     */
    public boolean awaitTunnelEstablished(long timeout, TimeUnit unit) throws InterruptedException
    {
        return tunnelLatch.await(timeout, unit);
    }

    /**
     * 检查隧道是否成功建立
     *
     * @return true 表示隧道已成功建立
     */
    public boolean isTunnelEstablished()
    {
        return tunnelEstablished;
    }

    /**
     * 获取隧道建立过程中的错误
     *
     * @return 错误信息，如果没有错误则返回 null
     */
    public Throwable getTunnelError()
    {
        return tunnelError;
    }

    /**
     * 设置隧道建立过程中的错误并通知等待线程
     * 用于外部在初始化过程中发生异常时调用
     *
     * @param error 错误信息
     */
    public void setTunnelError(Throwable error)
    {
        this.tunnelError = error;
        tunnelLatch.countDown();
    }
}
