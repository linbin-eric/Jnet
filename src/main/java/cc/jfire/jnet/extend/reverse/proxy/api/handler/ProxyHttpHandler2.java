package cc.jfire.jnet.extend.reverse.proxy.api.handler;

import cc.jfire.baseutil.STR;
import cc.jfire.baseutil.TRACEID;
import cc.jfire.jnet.client.ClientChannel;
import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessor;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.HeartBeat;
import cc.jfire.jnet.common.internal.DefaultPipeline;
import cc.jfire.jnet.common.util.ChannelConfig;
import cc.jfire.jnet.common.util.ReflectUtil;
import cc.jfire.jnet.extend.http.coder.HttpRequestPartEncoder;
import cc.jfire.jnet.extend.http.dto.*;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyHttpHandler2 implements ResourceHandler
{
    private final String          prefixMatch;
    private final int             prefixLen;
    private final String          proxy;
    private final String          backendHost;
    private final int             backendPort;
    private final String          backendHostHeader;
    private final String        backendBasePath;
    private       ClientChannel clientChannel;
    @Getter
    private       String          uid = TRACEID.newTraceId();
    private       String          pipelineUid;

    public ProxyHttpHandler2(String prefixMatch, String proxy)
    {
//        log.debug("[ProxyHttpHandler2] 初始化代理处理器, prefixMatch={}, proxy={}", prefixMatch, proxy);
        // 验证并处理前缀匹配规则
        if (!prefixMatch.endsWith("/*") || prefixMatch.chars().filter(c -> c == '*').count() != 1)
        {
//            log.error("[ProxyHttpHandler2] 前缀匹配规则不合规: {}", prefixMatch);
            throw new IllegalArgumentException(prefixMatch + "不是合规的前缀匹配地址");
        }
        this.prefixMatch = prefixMatch.substring(0, prefixMatch.length() - 1);
        this.prefixLen   = this.prefixMatch.length();
        this.proxy       = proxy;
        // 解析后端地址
        HttpUrl httpUrl = HttpUrl.parse(proxy);
        this.backendHost     = httpUrl.domain();
        this.backendPort     = httpUrl.port();
        this.backendBasePath = httpUrl.path();
        // 构造 Host header
        this.backendHostHeader = (backendPort == 80 || backendPort == 443) ? backendHost : (backendHost + ":" + backendPort);
//        log.info("[ProxyHttpHandler2] 代理处理器初始化完成, prefixMatch={}, backendHost={}, backendPort={}", this.prefixMatch, this.backendHost, this.backendPort);
    }

    @Override
    public boolean process(HttpRequestPart part, Pipeline pipeline)
    {
        if (pipelineUid == null)
        {
            pipelineUid = ((DefaultPipeline) pipeline).getUid();
        }
        else if (pipelineUid.equalsIgnoreCase(((DefaultPipeline) pipeline).getUid()) == false)
        {
            log.error("异常");
        }
//        log.debug("[ProxyHttpHandler2:{},pipeline:{}] process() 收到请求部分, partType={}, isLast={}", uid, pipelineUid, part.getClass().getSimpleName(), part.isLast());
        if (part instanceof HttpRequestPartHead head)
        {
            return processHead(head, pipeline);
        }
        else if (part instanceof HttpRequestFixLengthBodyPart || part instanceof HttpRequestChunkedBodyPart)
        {
            processBody(part);
            return true; // body部分始终返回true,因为已经在Head阶段匹配了
        }
        else
        {
//            log.warn("[ProxyHttpHandler2] process() 收到未知类型的请求部分: {}", part.getClass().getName());
            return false;
        }
    }

    private boolean processHead(HttpRequestPartHead head, Pipeline pipeline)
    {
//        log.debug("[ProxyHttpHandler2] processHead() 开始处理请求头, method={}, path={}, isLast={}", head.getMethod(), head.getPath(), head.isLast());
        // 计算后端路径
        String requestUrl = head.getPath();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        // 检查前缀是否匹配
        if (!requestUrl.startsWith(prefixMatch))
        {
//            log.trace("[ProxyHttpHandler2] processHead() 前缀不匹配, requestUrl={}, prefixMatch={}", requestUrl, prefixMatch);
            return false; // 不处理
        }
//        log.trace("[ProxyHttpHandler2] processHead() 前缀匹配, requestUrl={}, prefixMatch={}", requestUrl, prefixMatch);
        String backendPath = backendBasePath + requestUrl.substring(prefixLen);
        // 直接设置属性，避免重复解析
        head.setPath(backendPath);
        head.setDomain(backendHost);
        head.setPort(backendPort);
        // 替换 Host header（header name 已标准化，直接覆盖）
        head.getHeaders().put("Host", backendHostHeader);
        // 清空并释放 headBuffer，让编码器重新构建请求头
        IoBuffer old = head.getHeadBuffer();
        head.setHeadBuffer(null);
        if (old != null)
        {
            old.free();
        }
//        log.debug("[ProxyHttpHandler2:{}] processHead() URL转换完成, originalPath={}, backendPath={}", uid, head.getPath(), backendPath);
        // 首次请求时创建连接
        if (clientChannel == null)
        {
//            log.debug("[ProxyHttpHandler2:{}] processHead() 首次请求，创建后端连接, host={}, port={}", uid, backendHost, backendPort);
            try
            {
                ChannelConfig channelConfig = new ChannelConfig().setIp(backendHost).setPort(backendPort);
                clientChannel = ClientChannel.newClient(channelConfig, backendPipeline -> {
                    backendPipeline.addReadProcessor(new HeartBeat(1800, backendPipeline));
                    backendPipeline.addReadProcessor(new ReadProcessor<IoBuffer>()
                    {
                        @Override
                        public void read(IoBuffer buffer, ReadProcessorNode next)
                        {
                            if (!pipeline.isOpen())
                            {
                                buffer.free();
                                closeClientChannel();
                                return;
                            }
                            // 直接转发原始字节流到前端
                            pipeline.fireWrite(buffer);
                        }

                        @Override
                        public void readFailed(Throwable e, ReadProcessorNode next)
                        {
                            closeClientChannel();
                            pipeline.shutdownInput();
                        }
                    });
                    backendPipeline.addWriteProcessor(new HttpRequestPartEncoder());
                    backendPipeline.addWriteProcessor(new HeartBeat(1800, backendPipeline));
                });
                if (!clientChannel.connect())
                {
                    ReflectUtil.throwException(new RuntimeException("无法连接 " + backendHost + ":" + backendPort, clientChannel.getConnectionException()));
                }
//                log.info("[ProxyHttpHandler2:{}] processHead() 后端连接创建成功, host={}, port={}", uid, backendHost, backendPort);
            }
            catch (Exception e)
            {
//                log.error("[ProxyHttpHandler2:{}] processHead() 创建后端连接失败, host={}, port={}, error={}", uid, backendHost, backendPort, e.getMessage(), e);
                head.close();
                sendErrorResponse(pipeline, 502, "Bad Gateway - Cannot connect to backend");
                return true; // 已处理(返回错误响应)
            }
        }
        // 检查连接是否已关闭
        if (!clientChannel.alive())
        {
//            log.error("[ProxyHttpHandler2:{}] processHead() 后端连接已关闭，终止处理", uid);
            head.close();
            pipeline.shutdownInput();
            return true; // 已处理
        }
        try
        {
//            log.trace("[ProxyHttpHandler2:{}] processHead() 向后端发送请求头", uid);
            clientChannel.pipeline().fireWrite(head);
        }
        catch (Exception e)
        {
//            log.error("[ProxyHttpHandler2] processHead() 发送请求头异常, error={}", e.getMessage(), e);
            head.close();
            closeClientChannel();
            pipeline.shutdownInput();
            return true; // 已处理
        }
        return true; // 已处理
    }

    private void processBody(HttpRequestPart body)
    {
        if (body instanceof HttpRequestFixLengthBodyPart)
        {
//            log.trace("[{}] processBody() 处理请求体, bodyType=fix,长度:{}, isLast={}", toString(),((HttpRequestFixLengthBodyPart) body).getPart().remainRead(), body.isLast());
        }
        else if (body instanceof HttpRequestChunkedBodyPart)
        {
//            log.trace("[{}}] processBody() 处理请求体, bodyType=chunk,长度:{}, isLast={}",toString(), ((HttpRequestChunkedBodyPart) body).getPart().remainRead(), body.isLast());
        }
        if (clientChannel != null && clientChannel.alive())
        {
//            log.trace("[{}] processBody() 向后端转发请求体",toString());
            clientChannel.pipeline().fireWrite(body);
        }
        else
        {
//            log.warn("[{}] processBody() 后端连接不可用，丢弃请求体, connectionNull={}, connectionClosed={}",toString(), clientChannel == null, clientChannel != null && !clientChannel.alive());
            body.close();
        }
    }

    private void closeClientChannel()
    {
        if (clientChannel != null)
        {
//            log.debug("[{}] closeClientChannel() 关闭后端连接", toString());
            clientChannel.pipeline().shutdownInput();
            clientChannel = null;
        }
    }

    private void sendErrorResponse(Pipeline pipeline, int statusCode, String message)
    {
//        log.warn("[ProxyHttpHandler2] sendErrorResponse() 发送错误响应, statusCode={}, message={}", statusCode, message);
        FullHttpResp response = new FullHttpResp();
        response.getHead().setResponseCode(statusCode);
        response.getBody().setBodyText(message);
        pipeline.fireWrite(response);
    }

    @Override
    public void readFailed(Throwable e)
    {
        // 前端连接关闭，关闭后端连接
//        log.warn("[{}] readFailed() 前端读取失败，关闭后端连接, error={}", toString(), e != null ? e.getMessage() : "null", e);
        closeClientChannel();
    }

    @Override
    public String toString()
    {
        return STR.format("[Proxyhandler2:{},pipeline:{}]", uid, pipelineUid);
    }
}
