package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;
import lombok.ToString;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
@ToString(exclude = "body")
public class HttpRequest
{
    private String              method;
    private String              url;
    private String              version;
    private Map<String, String> headers = new HashMap<>();
    private int                 contentLength;
    private String              contentType;
    private IoBuffer            body;

    public record PathAndQueryParam(String path, Map<String, String> queryParams) {}

    public PathAndQueryParam parsePathAndQueryParam()
    {
        int index = url.indexOf("?");
        if (index == -1)
        {
            return new PathAndQueryParam(url, null);
        }
        else
        {
            String              path               = url.substring(0, index);
            String[]            paramNameAndValues = url.substring(index + 1).split("&");
            Map<String, String> map                = new HashMap<>();
            Arrays.stream(paramNameAndValues).forEach(v -> {
                int paramValueIndex = v.indexOf("=");
                if (paramValueIndex == -1)
                {
                    map.put(v, "");
                }
                else
                {
                    map.put(v.substring(0, paramValueIndex), v.substring(paramValueIndex + 1));
                }
            });
            return new PathAndQueryParam(path, map);
        }
    }

    public int matchBoundary(IoBuffer buffer, byte[] boundary)
    {
        int start = buffer.getReadPosi();
        int end   = buffer.getWritePosi() - boundary.length;
        for (int i = start; i < end; i++)
        {
            boolean found = true;
            for (int j = 0; j < boundary.length; j++)
            {
                if (buffer.get(i + j) != boundary[j])
                {
                    found = false;
                    break;
                }
            }
            if (found)
            {
                return i;
            }
        }
        return -1;
    }

    @Data
    public static class BoundaryData
    {
        private String   name;
        private String   value;
        private String   fileName;
        private IoBuffer binaryData;
        private boolean  isBinary;
    }

    public List<BoundaryData> parseMultipart()
    {
        if (contentType.toLowerCase().startsWith("multipart/form-data"))
        {
            try
            {
                byte[]         boundary      = ("--" + contentType.substring(contentType.indexOf("boundary=") + 9)).getBytes(StandardCharsets.US_ASCII);
                IoBuffer       buffer        = body;
                List<IoBuffer> elements      = new ArrayList<>();
                int            boundaryIndex = matchBoundary(buffer, boundary);
                if (boundaryIndex != 0)
                {
                    throw new IllegalArgumentException();
                }
                buffer.addReadPosi(boundary.length + 2);
                while (true)
                {
                    boundaryIndex = matchBoundary(buffer, boundary);
                    if (boundaryIndex != -1)
                    {
                        IoBuffer slice = buffer.slice(boundaryIndex - buffer.getReadPosi());
                        buffer.addReadPosi(boundary.length + 2);
                        elements.add(slice);
                    }
                    else
                    {
                        break;
                    }
                }
                buffer.free();
                List<BoundaryData> boundaryDataList = new ArrayList<>();
                for (IoBuffer element : elements)
                {
                    int contentLineEnd = 0;
                    for (int i = 0; ; i++)
                    {
                        if (element.get(i) == '\r' && element.get(i + 1) == '\n' && element.get(i + 2) == '\r' && element.get(i + 3) == '\n')
                        {
                            contentLineEnd = i + 2;
                            break;
                        }
                    }
                    BoundaryData boundaryData = new BoundaryData();
                    for (int i = 0; i < contentLineEnd; i++)
                    {
                        if (element.get(i) == '\r' && element.get(i + 1) == '\n')
                        {
                            IoBuffer slice    = element.slice(i - element.getReadPosi());
                            String   fragment = StandardCharsets.UTF_8.decode(slice.readableByteBuffer()).toString();
                            if (fragment.startsWith("Content-Disposition"))
                            {
                                int    left = fragment.indexOf("\"", fragment.indexOf("name="));
                                String name = fragment.substring(left + 1, fragment.indexOf("\"", left + 1));
                                boundaryData.setName(name);
                                int index = fragment.indexOf("filename=");
                                if (index != -1)
                                {
                                    boundaryData.setFileName(fragment.substring(index + 10, fragment.indexOf("\"", index + 10)));
                                    boundaryData.setBinary(true);
                                }
                            }
                            else if (fragment.startsWith("Content-Type:"))
                            {
                                if (fragment.contains("application/zip") || fragment.contains("application/java-archive") || fragment.contains("text/plain") || fragment.contains("text/x-yaml") || fragment.contains("application/octet-stream"))
                                {
                                    boundaryData.setBinary(true);
                                }
                            }
                            slice.free();
                            element.addReadPosi(2);
                        }
                    }
                    IoBuffer slice = element.addReadPosi(2).slice(element.remainRead() - 2);
                    if (boundaryData.isBinary)
                    {
                        boundaryData.setBinaryData(slice);
                    }
                    else
                    {
                        boundaryData.setValue(StandardCharsets.UTF_8.decode(slice.readableByteBuffer()).toString());
                        slice.free();
                    }
                    element.free();
                    boundaryDataList.add(boundaryData);
                }
                return boundaryDataList;
            }
            catch (Throwable e)
            {
                throw e;
            }
        }
        else
        {
            throw new IllegalArgumentException("not multipart");
        }
    }

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }
}
