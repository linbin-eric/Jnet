package cc.jfire.jnet.common.util;

import cc.jfire.jnet.common.buffer.allocator.impl.UnPoolBufferAllocator;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 当前类用于复现 HttpCoderUtil.findAllHeaders 的现有问题。
 *
 * 类名不匹配 Surefire 默认 *Test 规则，普通 mvn test 不会执行。
 * 需要单独复现时运行：
 * mvn -Dtest=cc.jfire.jnet.common.util.HttpCoderUtilProblemReproducer test
 */
public class HttpCoderUtilProblemReproducer
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
    public void loneCrCurrentlyReadsPastBufferEnd()
    {
        assertThrows(RuntimeException.class, () -> parse("\r"));
    }

    @Test
    public void colonWithoutValueCurrentlyReadsPastBufferEnd()
    {
        assertThrows(RuntimeException.class, () -> parse("X:"));
    }

    @Test
    public void headerWithoutColonCurrentlyProducesNullHeaderName()
    {
        Map<String, String> headers = parse("BrokenHeader\r\n\r\n");

        Assert.assertTrue(headers.containsKey(null));
        Assert.assertEquals("BrokenHeader", headers.get(null));
    }

    @Ignore("当前实现会进入不推进读指针的循环；修复后应改为受控失败断言。")
    @Test
    public void lfOnlyHeaderCurrentlyDoesNotReturn()
    {
        parse("Content-Type: text/plain\n\n");
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

    private interface ThrowingRunnable
    {
        void run() throws Throwable;
    }
}
