package cc.jfire.jnet.extend.http.client;

import org.jctools.queues.MpmcArrayQueue;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class HttpConnectionPool
{
    private static final int                               DEFAULT_MAX_CONNECTIONS_PER_HOST = 50;
    private static final int                               DEFAULT_TIMEOUT_SECONDS          = 1;
    private static final int                               KEEP_ALIVE_SECONDS               = 1800; // 30分钟
    private final        ConcurrentHashMap<String, Bucket> buckets                          = new ConcurrentHashMap<>();

    private String buildKey(String host, int port)
    {
        return host + ":" + port;
    }

    public HttpConnection borrowConnection(String host, int port) throws TimeoutException, InterruptedException
    {
        return borrowConnection(host, port, DEFAULT_TIMEOUT_SECONDS);
    }

    public HttpConnection borrowConnection(String host, int port, int timeoutSeconds) throws TimeoutException, InterruptedException
    {
        String key    = buildKey(host, port);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(DEFAULT_MAX_CONNECTIONS_PER_HOST));
        // 尽可能从队列获取现有连接
        HttpConnection connection;
        while ((connection = bucket.queue.poll()) != null)
        {
            if (!connection.isConnectionClosed())
            {
                return connection;
            }
            // 连接已失效，释放许可并继续尝试下一个
            bucket.semaphore.release();
        }
        // 队列为空，尝试获取信号量许可
        if (bucket.semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS))
        {
            // 需要创建新连接
            try
            {
                connection = new HttpConnection(host, port, KEEP_ALIVE_SECONDS);
                return connection;
            }
            catch (Exception e)
            {
                // 创建失败，释放许可
                bucket.semaphore.release();
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }
        else
        {
            // 超时未获取到许可
            throw new TimeoutException("无法在 " + timeoutSeconds + " 秒内获取到可用连接，地址: " + key);
        }
    }

    public void returnConnection(String host, int port, HttpConnection connection)
    {
        if (connection == null)
        {
            return;
        }
        String key    = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        if (bucket == null)
        {
            connection.close();
            return;
        }
        // 检查连接是否有效
        if (connection.isConnectionClosed())
        {
            // 连接已失效，释放许可
            bucket.semaphore.release();
            return;
        }
        // 检查连接是否有未完成的响应
        if (connection.hasUnfinishedResponse())
        {
            // 连接状态不干净，不能复用，关闭并释放许可
            connection.close();
            bucket.semaphore.release();
            return;
        }
        // 连接有效，尝试放入队列
        boolean offered = bucket.queue.offer(connection);
        if (!offered)
        {
            // 队列已满，关闭连接并释放许可
            connection.close();
            bucket.semaphore.release();
        }
    }

    /**
     * 获取当前连接总数（包括正在使用和队列中的）
     */
    public int getConnectionCount(String host, int port)
    {
        String key    = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.maxConnections - bucket.semaphore.availablePermits() : 0;
    }

    /**
     * 获取队列中的空闲连接数
     */
    public int getPoolSize(String host, int port)
    {
        String key    = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.queue.size() : 0;
    }

    /**
     * 获取可用的许可数（还能创建多少新连接）
     */
    public int getAvailablePermits(String host, int port)
    {
        String key    = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.semaphore.availablePermits() : 0;
    }

    private static class Bucket
    {
        private final Semaphore                      semaphore;
        private final MpmcArrayQueue<HttpConnection> queue;
        private final int                            maxConnections;

        public Bucket(int maxConnections)
        {
            this.maxConnections = maxConnections;
            this.semaphore      = new Semaphore(maxConnections);
            this.queue          = new MpmcArrayQueue<>(maxConnections);
        }
    }
}
