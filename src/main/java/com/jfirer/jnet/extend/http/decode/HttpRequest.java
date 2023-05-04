package com.jfirer.jnet.extend.http.decode;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.util.HttpDecodeUtil;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.ToString;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Data
@ToString(exclude = "body")
public class HttpRequest
{
    private String              method;
    private String              url;
    private String              version;
    private Map<String, String> headers       = new HashMap<>();
    private int                 contentLength = 0;
    private String              contentType;
    private IoBuffer            body;

    public record PathAndQueryParam(String path, Map<String, String> queryParams) {}

    public static final Map<String, String> DUMMY         = new HashMap<>();
    public static final byte[]              HEADER_END    = "/r/n/r/n".getBytes(StandardCharsets.US_ASCII);
    public static final int[]               HEADER_PREFIX = HttpDecodeUtil.computePrefix(HEADER_END);

    public PathAndQueryParam parsePathAndQueryParam()
    {
        int index = url.indexOf("?");
        if (index == -1)
        {
            return new PathAndQueryParam(url, DUMMY);
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

    @Data
    public static class BoundaryPart
    {
        @Setter(AccessLevel.NONE)
        private Map<String, String> headers  = new HashMap<>();
        private String              contentType;
        private String              fileName;
        private String              fieldName;
        private IoBuffer            data;
        private boolean             isBinary = false;

        public void putHeader(String header, String value)
        {
            headers.put(header, value);
        }

        public String getUTF8Value()
        {
            String value = StandardCharsets.UTF_8.decode(data.readableByteBuffer()).toString();
            data.free();
            data = null;
            return value;
        }

        public void analysisHeaders()
        {
            HttpDecodeUtil.findContentType(headers, this::setContentType);
            if (contentType == null)
            {
                contentType = "text/plain";
            }
            switch (contentType)
            {
                case "application/java-archive", "application/zip", ContentType.STREAM ->
                        isBinary = true;
            }
            String value       = headers.get("Content-Disposition");
            int    indexOfName = value.indexOf("name=");
            int    endOfIndex  = value.indexOf('"', indexOfName + 6);
            fieldName = value.substring(indexOfName + 6, endOfIndex);
            int index = value.indexOf("filename=");
            if (index != -1)
            {
                fileName = value.substring(index + 10, value.indexOf('"', index + 11));
                isBinary = true;
            }
            if ((index = value.indexOf("filename*=UTF-8''")) != -1)
            {
                fileName = URLDecoder.decode(value.substring(index + 17), StandardCharsets.UTF_8);
                isBinary = true;
            }
        }
    }

    public void addHeader(String name, String value)
    {
        headers.put(name, value);
    }

    public List<BoundaryPart> parseMultipart()
    {
        if (contentType.toLowerCase().startsWith("multipart/form-data"))
        {
            byte[]   boundary      = ("--" + contentType.substring(contentType.indexOf("boundary=") + 9)).getBytes(StandardCharsets.US_ASCII);
            int[]    prefix        = HttpDecodeUtil.computePrefix(boundary);
            IoBuffer buffer        = body;
            int      boundaryIndex = HttpDecodeUtil.findSubArray(buffer, boundary, prefix);
            if (boundaryIndex != buffer.getReadPosi())
            {
                throw new IllegalArgumentException();
            }
            buffer.addReadPosi(boundary.length + 2);
            List<BoundaryPart> boundaryPartList = new ArrayList<>();
            while (true)
            {
                boundaryIndex = HttpDecodeUtil.findSubArray(buffer, boundary, prefix);
                if (boundaryIndex != -1)
                {
                    IoBuffer     slice        = buffer.slice(boundaryIndex - buffer.getReadPosi());
                    BoundaryPart boundaryPart = new BoundaryPart();
                    HttpDecodeUtil.findAllHeaders(slice, boundaryPart::putHeader);
                    boundaryPart.setData(slice);
                    boundaryPart.analysisHeaders();
                    boundaryPartList.add(boundaryPart);
                    buffer.addReadPosi(boundary.length + 2);
                }
                else
                {
                    break;
                }
            }
            buffer.free();
            return boundaryPartList;
        }
        else
        {
            throw new IllegalArgumentException("not multipart");
        }
    }
}
