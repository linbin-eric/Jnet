package cc.jfire.jnet.extend.reverse.proxy.api.handler;

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
import cc.jfire.jnet.extend.watercheck.BackPresure;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProxyHttpHandler implements ResourceHandler
{
    public enum MatchMode
    {
        PREFIX,  // 前缀匹配
        EXACT    // 完全匹配
    }

    private final MatchMode     matchMode;
    private final String        match;        // 完全匹配时使用
    private final String        prefixMatch;
    private final int           prefixLen;
    private final String        backendHost;
    private final int           backendPort;
    private final String        backendHostHeader;
    private final String        backendBasePath;
    private       ClientChannel clientChannel;

    public ProxyHttpHandler(String match, String proxy, MatchMode matchMode)
    {
        this.matchMode = matchMode;
        // 解析后端地址
        HttpUrl httpUrl = HttpUrl.parse(proxy);
        this.backendHost     = httpUrl.domain();
        this.backendPort     = httpUrl.port();
        this.backendBasePath = httpUrl.path();
        // 构造 Host header
        this.backendHostHeader = (backendPort == 80 || backendPort == 443) ? backendHost : (backendHost + ":" + backendPort);
        if (matchMode == MatchMode.PREFIX)
        {
            // 前缀匹配：验证并处理前缀规则
            if (!match.endsWith("/*") || match.chars().filter(c -> c == '*').count() != 1)
            {
                throw new IllegalArgumentException(match + "不是合规的前缀匹配地址");
            }
            this.prefixMatch = match.substring(0, match.length() - 1);
            this.prefixLen   = this.prefixMatch.length();
            this.match       = null;
        }
        else
        {
            // 完全匹配：直接使用 match
            this.match       = match;
            this.prefixMatch = null;
            this.prefixLen   = 0;
        }
    }

    @Override
    public boolean process(HttpRequestPart part, Pipeline pipeline)
    {
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
            return false;
        }
    }

    private boolean processHead(HttpRequestPartHead head, Pipeline pipeline)
    {
        // 获取请求URL并去除 fragment
        String requestUrl = head.getPath();
        int    index      = requestUrl.indexOf("#");
        if (index != -1)
        {
            requestUrl = requestUrl.substring(0, index);
        }
        String backendPath;
        if (matchMode == MatchMode.PREFIX)
        {
            // 前缀匹配
            if (!requestUrl.startsWith(prefixMatch))
            {
                return false; // 不处理
            }
            backendPath = backendBasePath + requestUrl.substring(prefixLen);
        }
        else
        {
            // 完全匹配：提取路径部分（不含 query string）进行比较
            int    queryIndex = requestUrl.indexOf("?");
            String pathPart   = queryIndex == -1 ? requestUrl : requestUrl.substring(0, queryIndex);
            if (!pathPart.equals(match))
            {
                return false; // 不处理
            }
            // 后端路径 = backendBasePath + 原始 query string
            backendPath = queryIndex == -1 ? backendBasePath : backendBasePath + requestUrl.substring(queryIndex);
        }
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
        BackPresure upStreamBackpresure = (BackPresure) ((DefaultPipeline) pipeline).getPersistenceStore(BackPresure.UP_STREAM_BACKPRESURE);
        BackPresure inBackpresure       = (BackPresure) ((DefaultPipeline) pipeline).getPersistenceStore(BackPresure.IN_BACKPRESURE);
        // 首次请求时创建连接
        if (clientChannel == null)
        {
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
                    backendPipeline.addReadProcessor(upStreamBackpresure.readLimiter());
                    backendPipeline.addWriteProcessor(new HttpRequestPartEncoder());
                    backendPipeline.addWriteProcessor(new HeartBeat(1800, backendPipeline));
                    backendPipeline.setWriteListener(inBackpresure.writeLimiter());
                });
                if (!clientChannel.connect())
                {
                    ReflectUtil.throwException(new RuntimeException("无法连接 " + backendHost + ":" + backendPort, clientChannel.getConnectionException()));
                }
            }
            catch (Exception e)
            {
                head.close();
                sendErrorResponse(pipeline, 502, "Bad Gateway - Cannot connect to backend");
                return true; // 已处理(返回错误响应)
            }
        }
        // 检查连接是否已关闭
        if (!clientChannel.alive())
        {
            head.close();
            pipeline.shutdownInput();
            return true; // 已处理
        }
        try
        {
            clientChannel.pipeline().fireWrite(head);
        }
        catch (Exception e)
        {
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
        }
        else if (body instanceof HttpRequestChunkedBodyPart)
        {
        }
        if (clientChannel != null && clientChannel.alive())
        {
            clientChannel.pipeline().fireWrite(body);
        }
        else
        {
            body.close();
        }
    }

    private void closeClientChannel()
    {
        if (clientChannel != null)
        {
            clientChannel.pipeline().shutdownInput();
            clientChannel = null;
        }
    }

    private void sendErrorResponse(Pipeline pipeline, int statusCode, String message)
    {
        HttpResponse response = new HttpResponse();
        response.getHead().setStatusCode(statusCode);
        response.setBodyText(message);
        pipeline.fireWrite(response);
    }

    @Override
    public void readFailed(Throwable e)
    {
        // 前端连接关闭，关闭后端连接
        closeClientChannel();
    }

    @Override
    public void processWebSocket(IoBuffer buffer, Pipeline pipeline)
    {
        if (clientChannel != null && clientChannel.alive())
        {
            clientChannel.pipeline().fireWrite(buffer);
        }
        else
        {
            buffer.free();
        }
    }
}
