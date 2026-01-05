package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.extend.http.client.HttpConnection2;
import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public sealed abstract class ProxyHttpHandler implements ResourceHandler permits PrefixMatchProxyHttpHandler, FullMatchProxyHttpHandler
{
    protected final HttpConnection2Pool pool;

    /**
     * 后端连接上下文（用于归还/丢弃时带上 host/port）。
     */
    protected record BackendConn(String host, int port, HttpConnection2 conn)
    {
    }

    /**
     * 后端连接上下文，跨线程共享：
     * - 前端请求线程：borrow 后 set
     * - 后端响应 IO 线程 / readFailed 等：getAndSet(null) 清理
     * <p>
     * 使用 AtomicReference 保证“只清理/归还一次”，避免并发路径重复释放连接池许可。
     */
    protected final    AtomicReference<BackendConn> backendConnRef = new AtomicReference<>();
    protected          Pipeline                     currentPipeline;
    /**
     * 标记当前请求是否需要丢弃（用于拒绝 pipelining 等不支持场景）。
     * 为 true 时，Head/Body/End 都只做资源释放，不写入后端连接。
     */
    protected volatile boolean                      dropCurrentRequest;
    /**
     * 标记当前请求是否已收到客户端的 HttpRequestPartEnd。
     * 用于判断后端连接在收到 HttpResponsePartEnd 时是否可复用。
     */
    protected volatile boolean                      requestEndReceived;
    /**
     * 标记是否发生“后端响应 End 早于请求 End”（后端提前结束响应）。
     * 发生该情况时，后端连接不可复用，后续请求 body 仅做释放直到收到请求 End。
     */
    protected volatile boolean                      responseEndedBeforeRequestEnd;

    protected ProxyHttpHandler(HttpConnection2Pool pool)
    {
        this.pool = pool;
    }

    @Override
    public void process(HttpRequestPart part, Pipeline pipeline)
    {
        if (part instanceof HttpRequestPartHead head)
        {
            processHead(head, pipeline);
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
            processBody(part, pipeline);
        }
        else if (part instanceof HttpRequestPartEnd end)
        {
            processEnd(end, pipeline);
        }
    }

    /**
     * 子类实现，计算后端 URL 并设置到 head 中
     */
    protected abstract void computeBackendUrl(HttpRequestPartHead head);

    protected void closeAndReleaseBackendConn()
    {
        BackendConn ctx = backendConnRef.getAndSet(null);
        if (ctx == null)
        {
            return;
        }
        ctx.conn().close();
        pool.returnConnection(ctx.host(), ctx.port(), ctx.conn());
    }

    protected void releaseReusableBackendConn()
    {
        BackendConn ctx = backendConnRef.getAndSet(null);
        if (ctx == null)
        {
            return;
        }
        pool.returnConnection(ctx.host(), ctx.port(), ctx.conn());
    }

    protected void processHead(HttpRequestPartHead head, Pipeline pipeline)
    {
        // 不支持 HTTP pipelining：上一个后端响应未结束又收到新的请求
        if (backendConnRef.get() != null)
        {
            dropCurrentRequest = true;
            head.close();
            pipeline.shutdownInput();
            return;
        }
        dropCurrentRequest            = false;
        requestEndReceived            = false;
        responseEndedBeforeRequestEnd = false;
        this.currentPipeline          = pipeline;
        // 计算后端 URL（由子类实现）
        computeBackendUrl(head);
        // 后端连接信息由 computeBackendUrl -> head.setUrl(backendUrl) 填充
        String host = head.getDomain();
        int    port = head.getPort();
        if (host == null)
        {
            // 兜底：从 Host header 解析（历史兼容）
            host = head.getHeaders().get("Host");
            port = 80;
            if (host != null && host.contains(":"))
            {
                int idx = host.indexOf(':');
                port = Integer.parseInt(host.substring(idx + 1));
                host = host.substring(0, idx);
            }
        }
        try
        {
            HttpConnection2 httpConnection2 = pool.borrowConnection(host, port);
            backendConnRef.set(new BackendConn(host, port, httpConnection2));
            // 发送请求头，透传响应给客户端
            httpConnection2.write(head, responsePart -> {
                // 客户端连接已关闭：后端连接不可复用，直接关闭并释放连接池许可（不入池复用）
                if (!pipeline.isOpen())
                {
                    responsePart.free();
                    dropCurrentRequest = true;
                    closeAndReleaseBackendConn();
                    return;
                }
                if (responsePart instanceof HttpResponsePartHead respHead)
                {
                    // 透传对象本身，交由 CorsEncoder 注入 CORS，再由 HttpRespEncoder 编码写出
                    pipeline.fireWrite(respHead);
                }
                else if (responsePart instanceof HttpResponseFixLengthBodyPart body)
                {
                    pipeline.fireWrite(body.getPart());
                }
                else if (responsePart instanceof HttpResponseChunkedBodyPart body)
                {
                    pipeline.fireWrite(body.getPart());
                }
                else if (responsePart instanceof HttpResponsePartEnd)
                {
                    currentPipeline = null;
                    if (!requestEndReceived)
                    {
                        // 后端响应已结束但请求未结束：连接不可复用（避免后端协议状态错位）
                        dropCurrentRequest            = true;
                        responseEndedBeforeRequestEnd = true;
                        closeAndReleaseBackendConn();
                        return;
                    }
                    // 请求已结束：连接可复用
                    releaseReusableBackendConn();
                }
            }, error -> {
                log.error("后端请求失败", error);
                // 失败时关闭并归还连接（释放连接池许可）
                currentPipeline = null;
                closeAndReleaseBackendConn();
                // 客户端可能已关闭，写失败不应影响进程
                if (pipeline.isOpen())
                {
                    sendErrorResponse(pipeline, 502, "Bad Gateway");
                }
            });
        }
        catch (TimeoutException e)
        {
            log.error("获取连接超时: {}:{}", host, port, e);
            head.close();
            // backendConnRef 保持为空，后续会在 processEnd 中发送错误响应
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.error("获取连接被中断: {}:{}", host, port, e);
            head.close();
        }
        catch (Exception e)
        {
            log.error("获取连接失败: {}:{}", host, port, e);
            head.close();
            // 借到连接但 write 失败等情况，需关闭并归还（释放许可）
            currentPipeline = null;
            closeAndReleaseBackendConn();
        }
    }

    protected void processBody(HttpRequestPart body, Pipeline pipeline)
    {
        if (dropCurrentRequest)
        {
            body.close();
            return;
        }
        BackendConn ctx = backendConnRef.get();
        if (ctx == null)
        {
            body.close();
            return;
        }
        ctx.conn().write(body);
    }

    protected void processEnd(HttpRequestPartEnd end, Pipeline pipeline)
    {
        requestEndReceived = true;
        if (dropCurrentRequest)
        {
            dropCurrentRequest            = false;
            responseEndedBeforeRequestEnd = false;
            end.close();
            return;
        }
        if (backendConnRef.get() == null)
        {
            if (responseEndedBeforeRequestEnd)
            {
                // 后端已提前结束且连接已关闭/不可复用：此处仅做清理，避免误发 503
                responseEndedBeforeRequestEnd = false;
                end.close();
                return;
            }
            // 没有连接，发送"连接过多"响应
            sendErrorResponse(pipeline, 503, "Service Unavailable - Too Many Connections");
            return;
        }
        // 不在此处归还连接：归还时机以“收到后端 HttpResponsePartEnd”为准
    }

    protected void sendErrorResponse(Pipeline pipeline, int statusCode, String message)
    {
        FullHttpResp response = new FullHttpResp();
        response.getHead().setResponseCode(statusCode);
        response.getBody().setBodyText(message);
        pipeline.fireWrite(response);
    }

    @Override
    public void readFailed(Throwable e)
    {
        // 客户端断开/读取失败：后端连接不可复用，直接关闭并释放连接池许可
        currentPipeline               = null;
        dropCurrentRequest            = true;
        requestEndReceived            = false;
        responseEndedBeforeRequestEnd = false;
        closeAndReleaseBackendConn();
    }
}

