package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.WriteProcessorNode;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

/**
 * HTTP 代理请求编码器
 * 继承 HttpRequestPartEncoder，将请求行中的路径改为完整 URL（http://host:port/path）
 */
public class ProxyHttpRequestEncoder extends HttpRequestPartEncoder
{
    private final String targetHost;
    private final int    targetPort;

    public ProxyHttpRequestEncoder(String targetHost, int targetPort)
    {
        this.targetHost = targetHost;
        this.targetPort = targetPort;
    }

    /**
     * 重写 buildPath 方法，返回代理模式下的完整 URL
     */
    @Override
    protected String buildPath(HttpRequestPartHead head)
    {
        if (targetPort == 80)
        {
            return "http://" + targetHost + head.getPath();
        }
        else
        {
            return "http://" + targetHost + ":" + targetPort + head.getPath();
        }
    }

    /**
     * 重写 encodeHttpRequestPartHead 方法
     * 代理模式下不使用 headBuffer，总是重新编码以使用完整 URL
     */
    @Override
    protected void encodeHttpRequestPartHead(HttpRequestPartHead head, WriteProcessorNode next)
    {
        // 释放原始 headBuffer，强制重新编码
        if (head.getHeadBuffer() != null)
        {
            head.getHeadBuffer().free();
            head.setHeadBuffer(null);
        }
        // 调用父类方法进行编码（会使用重写的 buildPath 方法）
        super.encodeHttpRequestPartHead(head, next);
    }
}
