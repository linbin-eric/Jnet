package cc.jfire.jnet.extend.http.client;

import cc.jfire.baseutil.schedule.timer.SimpleWheelTimer;
import cc.jfire.baseutil.schedule.trigger.RepeatDelayTrigger;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@Slf4j
public class HttpConnectionPool
{
    private static final int                                                      MAX_CONNECTIONS_PER_HOST = 2;
    private final        ConcurrentHashMap<String, BlockingQueue<HttpConnection>> pools;
    private final        ConcurrentHashMap<String, AtomicInteger>                 connectionCounts;
    private final        ConcurrentHashMap<String, AtomicInteger>                 ids                      = new ConcurrentHashMap<>();
    private final        SimpleWheelTimer                                         timer;

    public HttpConnectionPool()
    {
        this.pools            = new ConcurrentHashMap<>();
        this.connectionCounts = new ConcurrentHashMap<>();
        this.timer            = new SimpleWheelTimer(Executors.newVirtualThreadPerTaskExecutor(), 1000 * 60 * 60);
        startConnectionChecker();
    }

    public HttpConnection borrowConnection(String host, int port, int timeoutSeconds, Consumer<HttpReceiveResponse> callback) throws TimeoutException, InterruptedException
    {
        String                        key   = buildKey(host, port);
        AtomicInteger                 idGen = ids.computeIfAbsent(key, k -> new AtomicInteger(1));
        BlockingQueue<HttpConnection> pool  = pools.computeIfAbsent(key, k -> new ArrayBlockingQueue<>(MAX_CONNECTIONS_PER_HOST));
        AtomicInteger                 count = connectionCounts.computeIfAbsent(key, k -> new AtomicInteger(0));
        // 首先尝试从池中获取可用连接
        HttpConnection connection = pool.poll();
        if (connection != null)
        {
            if (!connection.isConnectionClosed())
            {
                log.debug("地址:{}:{}连接:{}被借出,当前剩余:{}", host, port, connection.getId(), pool.size());
                connection.setLastBorrowTime(System.currentTimeMillis());
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
            int currentAvail = count.incrementAndGet();
            try
            {
                if (callback == null)
                {
                    connection = new HttpConnection(host, port, 60 * 30, idGen.getAndIncrement(), (pipeline, http) -> new HttpReceiveResponse(pipeline, http, null));
                }
                else
                {
                    connection = new HttpConnection(host, port, 60 * 30, idGen.getAndIncrement(), (pipeline, http) -> new HttpReceiveResponse(pipeline, http, callback));
                }
                log.debug("地址:{}:{}连接:{}被借出,当前创建:{}", host, port, connection.getId(), currentAvail);
                connection.setLastBorrowTime(System.currentTimeMillis());
                return connection;
            }
            catch (Exception e)
            {
                // 创建失败，回退计数
                count.decrementAndGet();
                throw new RuntimeException("创建连接失败: " + e.getMessage(), e);
            }
        }
        // 达到最大连接数，等待其他连接归还
        log.debug("地址:{}:{},达到最大连接数限制 {}, 等待可用连接", host, port, MAX_CONNECTIONS_PER_HOST);
        connection = pool.poll(timeoutSeconds, TimeUnit.SECONDS);
        if (connection != null)
        {
            if (!connection.isConnectionClosed())
            {
                log.debug("地址:{}:{}连接:{}被借出,当前队列:{}", host, port, connection.getId(), pool.size());
                connection.setLastBorrowTime(System.currentTimeMillis());
                return connection;
            }
            else
            {
                // 获取的连接已失效，减少计数并递归重试
                count.decrementAndGet();
                return borrowConnection(host, port, timeoutSeconds,callback);
            }
        }
        // 超时未获取到连接
        throw new TimeoutException("无法在 " + timeoutSeconds + " 秒内获取到可用连接，地址: " + key);
    }

    public void returnConnection(String host, int port, HttpConnection connection)
    {
        long lastBorrowTime = connection.getLastBorrowTime();
        if (connection == null || connection.isConnectionClosed())
        {
            if (connection != null)
            {
                String        key   = buildKey(host, port);
                AtomicInteger count = connectionCounts.get(key);
                if (count != null)
                {
                    int left = count.decrementAndGet();
                    log.debug("地址:{}:{},连接:{}被归还，已经失效，扣减有效数字,当前有效:{},当前队列:{}.花费时间:{}", host, port, connection.getId(), left, pools.get(key).size(), System.currentTimeMillis() - lastBorrowTime);
                }
            }
            return;
        }
        String                        key  = buildKey(host, port);
        BlockingQueue<HttpConnection> pool = pools.get(key);
        if (pool != null)
        {
            boolean offer = pool.offer(connection);
            if (!offer)
            {
                log.warn("地址:{}:{},连接:{}被归还，但无法加入队列，地址: {},花费时间:{}", host, port, connection.getId(), key, System.currentTimeMillis() - lastBorrowTime);
            }
            else
            {
                log.debug("地址:{}:{},连接:{}被归还，加入队列，当前队列:{},花费时间:{}", host, port, connection.getId(), pool.size(), System.currentTimeMillis() - lastBorrowTime);
            }
        }
        else
        {
            log.error("异常");
            System.exit(10);
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
            count.decrementAndGet();
            log.debug("连接:{}被移除，当前队列:{}", connection.getId(), pools.get(key).size());
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