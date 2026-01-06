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
    protected final AtomicReference<BackendConn> backendConnRef = new AtomicReference<>();

    /**
     * 请求丢弃模式枚举：
     * - NONE: 正常处理
     * - DROP_SILENT: 丢弃且不回复（pipelining 冲突、客户端已断开等）
     * - DROP_REPLY_503: 丢弃但需等请求结束后回复 503（获取连接超时/失败）
     */
    protected enum DropMode
    {
        NONE, DROP_SILENT, DROP_REPLY_503
    }

    protected volatile DropMode dropMode = DropMode.NONE;
    /**
     * 标记当前请求是否已收到客户端的 HttpRequestPartEnd。
     * 用于判断后端连接在收到 HttpResponsePartEnd 时是否可复用。
     */
    protected volatile boolean  requestEndReceived;

    protected ProxyHttpHandler(HttpConnection2Pool pool)
    {
        this.pool = pool;
    }

    @Override
    public void process(HttpRequestPart part, Pipeline pipeline)
    {
//        log.trace("[ProxyHttpHandler] process: {}, isLast: {}, dropMode: {}", part.getClass().getSimpleName(), part.isLast(), dropMode);
        // DROP_SILENT: 丢弃且不回复，直接释放资源
        if (dropMode == DropMode.DROP_SILENT)
        {
//            log.debug("[ProxyHttpHandler] DROP_SILENT模式, 丢弃请求部分");
            part.close();
            return;
        }
        // DROP_REPLY_503: 丢弃但需等请求结束后回复 503
        if (dropMode == DropMode.DROP_REPLY_503)
        {
//            log.debug("[ProxyHttpHandler] DROP_REPLY_503模式, 丢弃请求部分, isLast: {}", part.isLast());
            if (part.isLast())
            {
                sendErrorResponse(pipeline, 503, "Service Unavailable - Connection Timeout");
            }
            part.close();
            return;
        }
        if (part instanceof HttpRequestPartHead head)
        {
//            log.trace("[ProxyHttpHandler] 处理请求头: {} {}", head.getMethod(), head.getPath());
            processHead(head, pipeline);
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
//            log.trace("[ProxyHttpHandler] 处理请求体");
            processBody(part, pipeline);
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
//            log.warn("[ProxyHttpHandler] closeAndReleaseBackendConn: 无后端连接需要释放");
            return;
        }
//        log.trace("[ProxyHttpHandler] 关闭并释放后端连接: {}:{}", ctx.host(), ctx.port());
        ctx.conn().close();
        pool.returnConnection(ctx.host(), ctx.port(), ctx.conn());
    }

    protected void releaseReusableBackendConn()
    {
        BackendConn ctx = backendConnRef.getAndSet(null);
        if (ctx == null)
        {
//            log.trace("[ProxyHttpHandler] releaseReusableBackendConn: 无后端连接需要归还");
            return;
        }
//        log.trace("[ProxyHttpHandler] 归还可复用后端连接: {}:{}", ctx.host(), ctx.port());
        pool.returnConnection(ctx.host(), ctx.port(), ctx.conn());
    }

    protected void processHead(HttpRequestPartHead head, Pipeline pipeline)
    {
//        log.trace("[ProxyHttpHandler] processHead开始: {} {}", head.getMethod(), head.getPath());
        // 不支持 HTTP pipelining：上一个后端响应未结束又收到新的请求
        if (backendConnRef.get() != null)
        {
//            log.warn("[ProxyHttpHandler] 检测到HTTP pipelining, 拒绝处理");
            dropMode = DropMode.DROP_SILENT;
            head.close();
            pipeline.shutdownInput();
            return;
        }
        // 重置请求相关状态（dropMode 一旦非 NONE 就不重置，由 process() 入口统一处理）
        requestEndReceived = false;
        // 如果是无 body 请求，直接标记请求已结束
        if (head.isLast())
        {
            requestEndReceived = true;
//            log.trace("[ProxyHttpHandler] 无body请求, requestEndReceived=true");
        }
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
//            log.trace("[ProxyHttpHandler] 从Host header解析后端地址: {}:{}", host, port);
        }
//        log.trace("[ProxyHttpHandler] 准备连接后端: {}:{}", host, port);
        try
        {
            HttpConnection2 httpConnection2 = pool.borrowConnection(host, port);
//            log.trace("[ProxyHttpHandler] 成功借用后端连接: {}:{}", host, port);
            backendConnRef.set(new BackendConn(host, port, httpConnection2));
            // 发送请求头，透传响应给客户端
//            log.trace("[ProxyHttpHandler] 发送请求头到后端");
            httpConnection2.write(head, responsePart -> {
//                log.trace("[ProxyHttpHandler] 收到后端响应: {}", responsePart.getClass().getSimpleName());
                // 客户端连接已关闭：后端连接不可复用，直接关闭并释放连接池许可（不入池复用）
                if (!pipeline.isOpen())
                {
//                    log.warn("[ProxyHttpHandler] 客户端连接已关闭, 丢弃后端响应");
                    responsePart.free();
                    dropMode = DropMode.DROP_SILENT;
                    closeAndReleaseBackendConn();
                    return;
                }
                if (responsePart instanceof HttpResponsePartHead respHead)
                {
//                    log.trace("[ProxyHttpHandler] 透传响应头, statusCode: {}", respHead.getStatusCode());
                    // 透传对象本身，交由 CorsEncoder 注入 CORS，再由 HttpRespEncoder 编码写出
                    pipeline.fireWrite(respHead);
                    if (respHead.isLast())
                    {
                        handleResponseEnd(pipeline);
                    }
                }
                else if (responsePart instanceof HttpResponseFixLengthBodyPart body)
                {
//                    log.trace("[ProxyHttpHandler] 透传FixLength响应体, 大小: {}", body.getPart() != null ? body.getPart().remainRead() : 0);
                    pipeline.fireWrite(body.getPart());
                    if (body.isLast())
                    {
                        handleResponseEnd(pipeline);
                    }
                }
                else if (responsePart instanceof HttpResponseChunkedBodyPart body)
                {
//                    log.trace("[ProxyHttpHandler] 透传Chunked响应体, 大小: {}", body.getPart() != null ? body.getPart().remainRead() : 0);
                    pipeline.fireWrite(body.getPart());
                    if (body.isLast())
                    {
                        handleResponseEnd(pipeline);
                    }
                }
            }, error -> {
//                log.error("[ProxyHttpHandler] 后端请求失败", error);
                // 失败时关闭并归还连接（释放连接池许可）
                closeAndReleaseBackendConn();
                // 客户端可能已关闭，写失败不应影响进程
                if (pipeline.isOpen())
                {
                    sendErrorResponse(pipeline, 502, "Bad Gateway");
                }
            });
            // 无 body 请求：连接归还由响应回调中的 HttpResponsePartEnd 处理
        }
        catch (TimeoutException e)
        {
//            log.error("获取连接超时: {}:{}", host, port, e);
            head.close();
            // 设置为 DROP_REPLY_503，等请求结束后回复 503
            dropMode = DropMode.DROP_REPLY_503;
            // 如果是无 body 请求，直接发送 503
            if (head.isLast())
            {
                sendErrorResponse(pipeline, 503, "Service Unavailable - Connection Timeout");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
//            log.error("获取连接被中断: {}:{}", host, port, e);
            head.close();
            dropMode = DropMode.DROP_REPLY_503;
            if (head.isLast())
            {
                sendErrorResponse(pipeline, 503, "Service Unavailable - Connection Interrupted");
            }
        }
        catch (Exception e)
        {
//            log.error("获取连接失败: {}:{}", host, port, e);
            head.close();
            // 借到连接但 write 失败等情况，需关闭并归还（释放许可）
            closeAndReleaseBackendConn();
            dropMode = DropMode.DROP_REPLY_503;
            if (head.isLast())
            {
                sendErrorResponse(pipeline, 503, "Service Unavailable - Connection Failed");
            }
        }
    }

    private void handleResponseEnd(Pipeline pipeline)
    {
//        log.trace("[ProxyHttpHandler] 收到响应结束标记, requestEndReceived: {}", requestEndReceived);
        if (!requestEndReceived)
        {
            // 后端响应已结束但请求未结束：连接不可复用，后续 body 静默丢弃
//            log.warn("[ProxyHttpHandler] 响应结束但请求未结束, 连接不可复用");
            dropMode = DropMode.DROP_SILENT;
            closeAndReleaseBackendConn();
            return;
        }
        // 请求已结束：连接可复用
//        log.trace("[ProxyHttpHandler] 响应完成, 连接可复用");
        releaseReusableBackendConn();
    }

    protected void processBody(HttpRequestPart body, Pipeline pipeline)
    {
//        log.trace("[ProxyHttpHandler] processBody, isLast: {}", body.isLast());
        // 最后一个 body 写入前置位，避免并发下 response End 先到导致误判
        if (body.isLast())
        {
            requestEndReceived = true;
//            log.trace("[ProxyHttpHandler] 最后一个body, requestEndReceived=true");
        }
        // dropMode 的检查已在 process() 入口处理，这里无需重复
        BackendConn ctx = backendConnRef.get();
        if (ctx == null)
        {
            // 正常情况下不应走到这里（连接未建立时 dropMode 应为 DROP_REPLY_503）
//            log.warn("[ProxyHttpHandler] 无后端连接, 丢弃body");
            body.close();
            return;
        }
//        log.trace("[ProxyHttpHandler] 转发body到后端: {}:{}", ctx.host(), ctx.port());
        ctx.conn().write(body);
    }

    protected void sendErrorResponse(Pipeline pipeline, int statusCode, String message)
    {
//        log.trace("[ProxyHttpHandler] 发送错误响应: {} {}", statusCode, message);
        FullHttpResp response = new FullHttpResp();
        response.getHead().setResponseCode(statusCode);
        response.getBody().setBodyText(message);
        pipeline.fireWrite(response);
    }

    @Override
    public void readFailed(Throwable e)
    {
//        log.error("[ProxyHttpHandler] readFailed, 客户端断开或读取失败", e);
        // 客户端断开/读取失败：后端连接不可复用，直接关闭并释放连接池许可
        dropMode           = DropMode.DROP_SILENT;
        requestEndReceived = false;
        closeAndReleaseBackendConn();
    }
}

