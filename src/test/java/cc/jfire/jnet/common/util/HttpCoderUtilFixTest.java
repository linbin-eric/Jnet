package cc.jfire.jnet.common.util;

import cc.jfire.jnet.common.buffer.allocator.impl.UnPoolBufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import org.junit.Assert;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 修复验收测试：当前未修复状态下应失败，修复后应全部通过。
 */
public class HttpCoderUtilFixTest
{
    @Test
    public void validHeadersCanBeParsed()
    {
        Map<String, String> headers = parse("Host: example.com\r\nContent-Type: text/plain\r\n\r\n");

        Assert.assertEquals("example.com", headers.get("Host"));
        Assert.assertEquals("text/plain", headers.get("Content-Type"));
    }

    @Test
    public void emptyHeaderBlockCanBeParsed()
    {
        Map<String, String> headers = parse("\r\n");

        Assert.assertTrue(headers.isEmpty());
    }

    @Test
    public void loneCrShouldFailWithControlledHeaderError()
    {
        assertInvalidHeader("\r");
    }

    @Test
    public void incompleteHeaderLineShouldFailWithControlledHeaderError()
    {
        assertInvalidHeader("X:");
    }

    @Test
    public void headerWithoutColonShouldFailWithControlledHeaderError()
    {
        assertInvalidHeader("BrokenHeader\r\n\r\n");
    }

    @Test
    public void lfOnlyHeaderShouldFailWithoutHanging()
    {
        assertInvalidHeader("Content-Type: text/plain\n\n");
    }

    private void assertInvalidHeader(String content)
    {
        assertCompletesWithin(500, () -> {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> parse(content));
            Assert.assertTrue("异常信息应说明是 HTTP header 格式错误，实际：" + e.getMessage(), e.getMessage() != null && e.getMessage().contains("Invalid HTTP header"));
        });
    }

    private Map<String, String> parse(String content)
    {
        IoBuffer buffer = buffer(content);
        Map<String, String> headers = new LinkedHashMap<>();
        try
        {
            HttpCoderUtil.findAllHeaders(buffer, headers::put);
            return headers;
        }
        finally
        {
            buffer.free();
        }
    }

    private IoBuffer buffer(String content)
    {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        IoBuffer buffer = UnPoolBufferAllocator.DEFAULT.allocate(bytes.length);
        buffer.put(bytes);
        return buffer;
    }

    private static void assertCompletesWithin(long timeoutMillis, ThrowingRunnable runnable)
    {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();
        Thread thread = new Thread(() -> {
            try
            {
                runnable.run();
            }
            catch (Throwable e)
            {
                error.set(e);
            }
            finally
            {
                latch.countDown();
            }
        }, "http-header-fix-test");
        thread.setDaemon(true);
        thread.start();
        try
        {
            if (!latch.await(timeoutMillis, TimeUnit.MILLISECONDS))
            {
                throw new AssertionError("解析未在 " + timeoutMillis + "ms 内返回");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new AssertionError("等待解析完成时被中断", e);
        }
        if (error.get() != null)
        {
            throwUnchecked(error.get());
        }
    }

    private static <T extends Throwable> T assertThrows(Class<T> expectedType, ThrowingRunnable runnable)
    {
        try
        {
            runnable.run();
        }
        catch (Throwable actual)
        {
            if (expectedType.isInstance(actual))
            {
                return expectedType.cast(actual);
            }
            throw new AssertionError("Expected " + expectedType.getName() + " but got " + actual.getClass().getName(), actual);
        }
        throw new AssertionError("Expected " + expectedType.getName() + " to be thrown");
    }

    private static void throwUnchecked(Throwable e)
    {
        if (e instanceof RuntimeException runtimeException)
        {
            throw runtimeException;
        }
        if (e instanceof Error error)
        {
            throw error;
        }
        throw new AssertionError(e);
    }

    private interface ThrowingRunnable
    {
        void run() throws Throwable;
    }
}
