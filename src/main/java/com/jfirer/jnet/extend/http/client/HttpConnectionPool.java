package com.jfirer.jnet.extend.http.client;

import com.jfirer.baseutil.schedule.timer.SimpleWheelTimer;
import com.jfirer.baseutil.schedule.trigger.RepeatDelayTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class HttpConnectionPool
{
    private static final int                                                      MAX_CONNECTIONS_PER_HOST  = 10;
    private final        ConcurrentHashMap<String, BlockingQueue<HttpConnection>> pools;
    private final        ConcurrentHashMap<String, AtomicInteger>                 connectionCounts;
    private final        SimpleWheelTimer                                         timer;

    public HttpConnectionPool()
    {
        this.pools            = new ConcurrentHashMap<>();
        this.connectionCounts = new ConcurrentHashMap<>();
        this.timer            = new SimpleWheelTimer(Executors.newVirtualThreadPerTaskExecutor(), 1000 * 30);
        startConnectionChecker();
    }

    public HttpConnection borrowConnection(String host, int port, int timeoutSeconds) throws TimeoutException, InterruptedException
    {
        String                        key   = buildKey(host, port);
        BlockingQueue<HttpConnection> pool  = pools.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_HOST));
        AtomicInteger                 count = connectionCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        // 首先尝试从池中获取可用连接
        HttpConnection connection = pool.poll();
        if (connection != null)
        {
            if (!connection.isConnectionClosed())
            {
                return connection;
            }
            else
            {
                // 连接已失效，减少计数
                count.decrementAndGet();
            }
        }
        // 如果没有可用连接，检查是否可以创建新连接
        if (count.get() < MAX_CONNECTIONS_PER_HOST)
        {
            // 可以创建新连接
            count.incrementAndGet();
            try
            {
                return new HttpConnection(host, port, 60 * 5);
            }
            catch (Exception e)
            {
                // 创建失败，回退计数
                count.decrementAndGet();
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }
        // 达到最大连接数，等待其他连接归还
        log.debug("达到最大连接数限制 {}, 等待可用连接", MAX_CONNECTIONS_PER_HOST);
        connection = pool.poll(timeoutSeconds, TimeUnit.SECONDS);
        if (connection != null)
        {
            if (!connection.isConnectionClosed())
            {
                return connection;
            }
            else
            {
                // 获取的连接已失效，减少计数并递归重试
                count.decrementAndGet();
                return borrowConnection(host, port, timeoutSeconds);
            }
        }
        // 超时未获取到连接
        throw new TimeoutException("无法在 " + timeoutSeconds + " 秒内获取到可用连接，地址: " + key);
    }

    public void returnConnection(String host, int port, HttpConnection connection)
    {
        if (connection == null || connection.isConnectionClosed())
        {
            if (connection != null)
            {
                String        key   = buildKey(host, port);
                AtomicInteger count = connectionCounts.get(key);
                if (count != null)
                {
                    count.decrementAndGet();
                }
            }
            return;
        }
        
        String                        key  = buildKey(host, port);
        BlockingQueue<HttpConnection> pool = pools.get(key);
        if (pool != null)
        {
            pool.offer(connection);
        }
        else
        {
            connection.close();
            AtomicInteger count = connectionCounts.get(key);
            if (count != null)
            {
                count.decrementAndGet();
            }
        }
    }

    public void removeConnection(String host, int port, HttpConnection connection)
    {
        if (connection != null)
        {
            connection.close();
            String        key   = buildKey(host, port);
            AtomicInteger count = connectionCounts.get(key);
            if (count != null)
            {
                count.decrementAndGet();
            }
        }
    }

    private void startConnectionChecker()
    {
        timer.add(new RepeatDelayTrigger(() -> {
            for (String key : pools.keySet())
            {
                BlockingQueue<HttpConnection> pool  = pools.get(key);
                AtomicInteger                 count = connectionCounts.get(key);
                if (pool != null && count != null)
                {
                    int initialSize = pool.size();
                    pool.removeIf(conn -> {
                        if (conn.isConnectionClosed())
                        {
                            count.decrementAndGet();
                            return true;
                        }
                        return false;
                    });
                    int removedCount = initialSize - pool.size();
                    if (removedCount > 0)
                    {
                        log.debug("清理了 {} 个无效连接，地址: {}", removedCount, key);
                    }
                }
            }
        }, 60, TimeUnit.SECONDS));
    }

    private String buildKey(String host, int port)
    {
        return host + ":" + port;
    }

    public int getConnectionCount(String host, int port)
    {
        String        key   = buildKey(host, port);
        AtomicInteger count = connectionCounts.get(key);
        return count != null ? count.get() : 0;
    }

    public int getPoolSize(String host, int port)
    {
        String                        key  = buildKey(host, port);
        BlockingQueue<HttpConnection> pool = pools.get(key);
        return pool != null ? pool.size() : 0;
    }
}