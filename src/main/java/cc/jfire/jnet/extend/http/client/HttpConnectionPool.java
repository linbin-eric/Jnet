package cc.jfire.jnet.extend.http.client;

import org.jctools.queues.MpmcArrayQueue;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

@Slf4j
public class HttpConnectionPool
{
    private static final int DEFAULT_MAX_CONNECTIONS_PER_HOST = 50;
    private static final int DEFAULT_TIMEOUT_SECONDS = 1;
    private static final int KEEP_ALIVE_SECONDS = 1800; // 30分钟

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    private static class Bucket
    {
        private final Semaphore                      semaphore;
        private final MpmcArrayQueue<HttpConnection> queue;
        private final int                            maxConnections;

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

    public HttpConnection borrowConnection(String host, int port) throws TimeoutException, InterruptedException
    {
        return borrowConnection(host, port, DEFAULT_TIMEOUT_SECONDS);
    }

    public HttpConnection borrowConnection(String host, int port, int timeoutSeconds) throws TimeoutException, InterruptedException
    {
        String key = buildKey(host, port);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(DEFAULT_MAX_CONNECTIONS_PER_HOST));

        // 尽可能从队列获取现有连接
        HttpConnection connection;
        while ((connection = bucket.queue.poll()) != null)
        {
            if (!connection.isConnectionClosed())
            {
                log.debug("地址:{}从队列借出连接，当前队列剩余:{},当前许可总数:{}", key, bucket.queue.size(),bucket.semaphore.availablePermits());
                return connection;
            }
            // 连接已失效，释放许可并继续尝试下一个
            bucket.semaphore.release();
//            log.debug("地址:{}队列中的连接已失效，已清理", key);
        }

        // 队列为空，尝试获取信号量许可
        if (bucket.semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS))
        {

            // 需要创建新连接
            try
            {
                connection = new HttpConnection(host, port, KEEP_ALIVE_SECONDS);
//                log.debug("地址:{}创建新连接，当前许可数量:{}", key,  bucket.semaphore.availablePermits());
                return connection;
            }
            catch (Exception e)
            {
                log.error("地址:{}创建连接失败: " + e.getMessage(), key, e);
                // 创建失败，释放许可
                bucket.semaphore.release();
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }
        else
        {
            log.error("地址:{}无法在 " + timeoutSeconds + " 秒内获取到可用连接，已释放许可，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
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

        String key = buildKey(host, port);
        Bucket bucket = buckets.get(key);

        if (bucket == null)
        {
//            log.warn("地址:{}的Bucket不存在，关闭连接", key);
            connection.close();
            return;
        }

        // 检查连接是否有效
        if (connection.isConnectionClosed())
        {
            // 连接已失效，释放许可
            bucket.semaphore.release();
//            log.debug("地址:{}归还的连接已失效，已释放许可，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
            return;
        }

        // 检查连接是否有未完成的响应
        if (connection.hasUnfinishedResponse())
        {
            // 连接状态不干净，不能复用，关闭并释放许可
            connection.close();
            bucket.semaphore.release();
//            log.warn("地址:{}归还的连接有未完成的响应，已关闭并释放许可，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits(),new IllegalStateException());
            return;
        }

        // 连接有效，尝试放入队列
        boolean offered = bucket.queue.offer(connection);
        if (offered)
        {
//            log.debug("地址:{}归还连接到队列，当前队列大小:{}", key, bucket.queue.size());
        }
        else
        {
            // 队列已满，关闭连接并释放许可
            connection.close();
            bucket.semaphore.release();
//            log.warn("地址:{}队列已满，关闭归还的连接，当前连接总数:{}", key, bucket.maxConnections - bucket.semaphore.availablePermits());
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
