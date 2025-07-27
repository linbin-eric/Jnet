package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.api.Pipeline;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketTimeoutException;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class PooledHttpReceiveResponse extends HttpReceiveResponse
{
    private final HttpReceiveResponse delegate;
    private final HttpConnectionPool  connectionPool;
    private final HttpConnection      connection;
    private final String              host;
    private final int                 port;
    private final AtomicBoolean       connectionReturned = new AtomicBoolean(false);

    public PooledHttpReceiveResponse(HttpReceiveResponse delegate, HttpConnectionPool connectionPool, HttpConnection connection, String host, int port)
    {
        // 调用父类构造函数
        super(delegate.getPipeline());
        this.delegate       = delegate;
        this.connectionPool = connectionPool;
        this.connection     = connection;
        this.host           = host;
        this.port           = port;
        // 设置完成回调，在响应真正完成时归还连接
        delegate.setCompletionCallback(this::onResponseCompleted);
    }

    private void onResponseCompleted(HttpReceiveResponse response)
    {
        // 确保连接只归还一次
        if (connectionReturned.compareAndSet(false, true))
        {
            try
            {
                connectionPool.returnConnection(host, port, connection);
                log.debug("响应完成，连接已归还到连接池: {}:{}", host, port);
            }
            catch (Exception e)
            {
                log.warn("归还连接到连接池时发生异常: {}:{}", host, port, e);
                // 如果归还失败，尝试移除连接
                try
                {
                    connectionPool.removeConnection(host, port, connection);
                }
                catch (Exception ex)
                {
                    log.warn("移除连接时发生异常: {}:{}", host, port, ex);
                }
            }
        }
    }
    // 委托所有方法到原始对象

    @Override
    public Pipeline getPipeline()
    {
        return delegate.getPipeline();
    }

    @Override
    public int getHttpCode()
    {
        return delegate.getHttpCode();
    }

    @Override
    public void setHttpCode(int httpCode)
    {
        delegate.setHttpCode(httpCode);
    }

    @Override
    public Map<String, String> getHeaders()
    {
        return delegate.getHeaders();
    }

    @Override
    public void setHeaders(Map<String, String> headers)
    {
        delegate.setHeaders(headers);
    }

    @Override
    public int getContentLength()
    {
        return delegate.getContentLength();
    }

    @Override
    public void setContentLength(int contentLength)
    {
        delegate.setContentLength(contentLength);
    }

    @Override
    public String getContentType()
    {
        return delegate.getContentType();
    }

    @Override
    public void setContentType(String contentType)
    {
        delegate.setContentType(contentType);
    }

    @Override
    public BlockingQueue<Part> getBody()
    {
        return delegate.getBody();
    }

    @Override
    public void setBody(BlockingQueue<Part> body)
    {
        delegate.setBody(body);
    }

    @Override
    public void putHeader(String name, String value)
    {
        delegate.putHeader(name, value);
    }

    @Override
    public void addPartOfBody(Part part)
    {
        delegate.addPartOfBody(part);
    }

    @Override
    public void endOfBody()
    {
        delegate.endOfBody();
    }

    @Override
    public boolean waitForReceiveFinish(long msOfReadTimeout)
    {
        return delegate.waitForReceiveFinish(msOfReadTimeout);
    }

    @Override
    public String getCachedUTF8Body(long msOfReadTimeout) throws SocketTimeoutException
    {
        return delegate.getCachedUTF8Body(msOfReadTimeout);
    }

    @Override
    public Part pollChunk(long timeout) throws InterruptedException
    {
        return delegate.pollChunk(timeout);
    }

    @Override
    public void close()
    {
        try
        {
            // 调用原始对象的 close 方法
            delegate.close();
        }
        catch (Exception e)
        {
            log.warn("关闭 HttpReceiveResponse 时发生异常", e);
        }
        // 注意：连接归还逻辑现在在完成回调中处理，不在这里
        // 但如果用户调用 close() 而响应还未完成，我们需要处理这种情况
        if (!connectionReturned.get())
        {
            log.debug("用户提前关闭响应，等待数据传输完成后归还连接");
            // 连接会在 endOfBody() 触发的回调中归还
        }
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PooledHttpReceiveResponse that = (PooledHttpReceiveResponse) obj;
        return delegate.equals(that.delegate);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public String toString()
    {
        return "PooledHttpReceiveResponse{" + "host='" + host + '\'' + ", port=" + port + ", connectionReturned=" + connectionReturned.get() + ", delegate=" + delegate + '}';
    }
}