package cc.jfire.jnet.extend.http.client;

import org.jctools.queues.MpmcArrayQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class HttpConnection2Pool
{
    private static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 10;
    private static final int DEFAULT_TIMEOUT_SECONDS = 5;
    private static final int KEEP_ALIVE_SECONDS = 1800; // 30分钟

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static class Bucket
    {
        private final Semaphore semaphore;
        private final MpmcArrayQueue<HttpConnection2> queue;
        private final int maxConnections;

        public Bucket(int maxConnections)
        {
            this.maxConnections = maxConnections;
            this.semaphore = new Semaphore(maxConnections);
            this.queue = new MpmcArrayQueue<>(maxConnections);
        }
    }

    private String buildKey(String host, int port)
    {
        return host + ":" + port;
    }

    public HttpConnection2 borrowConnection(String host, int port) throws TimeoutException, InterruptedException
    {
        return borrowConnection(host, port, DEFAULT_TIMEOUT_SECONDS);
    }

    public HttpConnection2 borrowConnection(String host, int port, int timeoutSeconds) throws TimeoutException, InterruptedException
    {
        String key = buildKey(host, port);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(DEFAULT_MAX_CONNECTIONS_PER_HOST));

        // 首先尝试从队列获取现有连接
        HttpConnection2 connection = bucket.queue.poll();
        if (connection != null)
        {
            if (!connection.isConnectionClosed())
            {
                log.debug("地址:{}从队列借出连接，当前队列剩余:{}", key, bucket.queue.size());
                return connection;
            }
            else
            {
                // 连接已失效，释放许可
                bucket.semaphore.release();
                log.debug("地址:{}队列中的连接已失效，已清理", key);
            }
        }

        // 队列为空，尝试获取信号量许可
        if (bucket.semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS))
        {
            // 获取许可后再次尝试从队列获取（可能其他线程刚归还）
            connection = bucket.queue.poll();
            if (connection != null && !connection.isConnectionClosed())
            {
                bucket.semaphore.release(); // 释放许可，因为使用的是已有连接
                log.debug("地址:{}从队列借出连接（二次尝试），当前队列剩余:{}", key, bucket.queue.size());
                return connection;
            }

            // 需要创建新连接
            try
            {
                connection = new HttpConnection2(host, port, KEEP_ALIVE_SECONDS);
                log.debug("地址:{}创建新连接，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
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

    public void returnConnection(String host, int port, HttpConnection2 connection)
    {
        if (connection == null)
        {
            return;
        }

        String key = buildKey(host, port);
        Bucket bucket = buckets.get(key);

        if (bucket == null)
        {
            log.warn("地址:{}的Bucket不存在，关闭连接", key);
            connection.close();
            return;
        }

        // 检查连接是否有效
        if (connection.isConnectionClosed())
        {
            // 连接已失效，释放许可
            bucket.semaphore.release();
            log.debug("地址:{}归还的连接已失效，已释放许可，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
            return;
        }

        // 连接有效，尝试放入队列
        boolean offered = bucket.queue.offer(connection);
        if (offered)
        {
            log.debug("地址:{}归还连接到队列，当前队列大小:{}", key, bucket.queue.size());
        }
        else
        {
            // 队列已满，关闭连接并释放许可
            connection.close();
            bucket.semaphore.release();
            log.warn("地址:{}队列已满，关闭归还的连接，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
        }
    }

    /**
     * 获取当前连接总数（包括正在使用和队列中的）
     */
    public int getConnectionCount(String host, int port)
    {
        String key = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.maxConnections - bucket.semaphore.availablePermits() : 0;
    }

    /**
     * 获取队列中的空闲连接数
     */
    public int getPoolSize(String host, int port)
    {
        String key = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.queue.size() : 0;
    }

    /**
     * 获取可用的许可数（还能创建多少新连接）
     */
    public int getAvailablePermits(String host, int port)
    {
        String key = buildKey(host, port);
        Bucket bucket = buckets.get(key);
        return bucket != null ? bucket.semaphore.availablePermits() : 0;
    }
}
