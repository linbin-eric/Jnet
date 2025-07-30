package com.jfirer.jnet.common.util;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.extend.reverse.proxy.ContentTypeDist;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class HttpDecodeUtil
{
    public static int findSubArray(IoBuffer buffer, byte[] B, int[] prefix)
    {
        int j   = 0;
        int end = buffer.getWritePosi();
        for (int i = buffer.getReadPosi(); i < end; i++)
        {
            while (j > 0 && B[j] != buffer.get(i))
            {
                j = prefix[j - 1];
            }
            if (B[j] == buffer.get(i))
            {
                j++;
            }
            if (j == B.length)
            {
                return i - j + 1;
            }
        }
        return -1;
    }

    public static int findSubarray(byte[] A, byte[] B)
    {
        int[] prefix = computePrefix(B);
        int   j      = 0;
        for (int i = 0; i < A.length; i++)
        {
            while (j > 0 && B[j] != A[i])
            {
                j = prefix[j - 1];
            }
            if (B[j] == A[i])
            {
                j++;
            }
            if (j == B.length)
            {
                return i - j + 1;
            }
        }
        return -1;
    }

    public static int[] computePrefix(byte[] B)
    {
        int[] prefix = new int[B.length];
        int   j      = 0;
        for (int i = 1; i < B.length; i++)
        {
            while (j > 0 && B[j] != B[i])
            {
                j = prefix[j - 1];
            }
            if (B[j] == B[i])
            {
                j++;
            }
            prefix[i] = j;
        }
        return prefix;
    }

    public static void findAllHeaders(IoBuffer ioBuffer, BiConsumer<String, String> consumer)
    {
        String headerName = null, headerValue = null;
        while (ioBuffer.get(ioBuffer.getReadPosi()) != '\r' || ioBuffer.get(ioBuffer.getReadPosi() + 1) != '\n')
        {
            for (int i = ioBuffer.getReadPosi(); i < ioBuffer.getWritePosi(); i++)
            {
                if (ioBuffer.get(i) == ':')
                {
                    headerName = StandardCharsets.US_ASCII.decode(ioBuffer.readableByteBuffer(i)).toString();
                    ioBuffer.setReadPosi(i + 2);
                    break;
                }
            }
            for (int i = ioBuffer.getReadPosi(); i < ioBuffer.getWritePosi(); i++)
            {
                if (ioBuffer.get(i) == '\r')
                {
                    headerValue = StandardCharsets.US_ASCII.decode(ioBuffer.readableByteBuffer(i)).toString();
                    ioBuffer.setReadPosi(i + 2);
                    break;
                }
            }
            consumer.accept(headerName, headerValue);
        }
        ioBuffer.addReadPosi(2);
    }

    public static void findContentType(Map<String, String> headers, Consumer<String> contentTypeConsumer)
    {
        headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Content-Type")).map(Map.Entry::getValue).findFirst().ifPresent(contentTypeConsumer);
    }

    public static void findContentLength(Map<String, String> headers, Consumer<Integer> contentLengthConsumer)
    {
        headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase("Content-Length")).map(entry -> Integer.valueOf(entry.getValue())).findFirst().ifPresent(contentLengthConsumer);
    }

    public static String pureUrl(String url)
    {
        int index = url.indexOf("#");
        if (index != -1)
        {
            url = url.substring(0, index);
        }
        index = url.indexOf("?");
        if (index != -1)
        {
            url = url.substring(0, index);
        }
        return url;
    }

    public static String findContentType(String url)
    {
        String contentType;
        int    i = url.lastIndexOf(".");
        if (i == -1)
        {
            contentType = "text/html";
        }
        else
        {
            contentType = ContentTypeDist.getOrDefault(url.substring(i), "text/html");
        }
        return contentType;
    }
}
