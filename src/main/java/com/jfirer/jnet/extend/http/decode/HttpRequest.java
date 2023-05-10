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
public class HttpRequest implements AutoCloseable
{
    protected           String              method;
    protected           String              url;
    protected           String              version;
    protected           Map<String, String> headers       = new HashMap<>();
    protected           int                 contentLength = 0;
    protected           String              contentType;
    protected           IoBuffer            body;
    protected           List<BoundaryPart>  parts         = DUMMY_PARTS;
    public static final List<BoundaryPart>  DUMMY_PARTS   = new LinkedList<>();

    public void close()
    {
        if (body != null)
        {
            body.free();
        }
        parts.forEach(each -> each.close());
    }

    public void parseMaybeMutliparts()
    {
        if (contentType != null && contentType.toLowerCase().startsWith("multipart/form-data"))
        {
            byte[] boundary      = ("--" + contentType.substring(contentType.indexOf("boundary=") + 9)).getBytes(StandardCharsets.US_ASCII);
            int[]  prefix        = HttpDecodeUtil.computePrefix(boundary);
            int    boundaryIndex = HttpDecodeUtil.findSubArray(body, boundary, prefix);
            if (boundaryIndex != body.getReadPosi())
            {
                throw new IllegalArgumentException();
            }
            body.addReadPosi(boundary.length + 2);
            parts = new ArrayList<>();
            while (true)
            {
                boundaryIndex = HttpDecodeUtil.findSubArray(body, boundary, prefix);
                if (boundaryIndex != -1)
                {
                    //数据范围需要将回车换行去掉
                    IoBuffer slice = body.slice(boundaryIndex - body.getReadPosi() - 2);
                    parts.add(new BoundaryPart(slice));
                    body.addReadPosi(2 + boundary.length + 2);
                }
                else
                {
                    break;
                }
            }
            body.free();
            body = null;
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
        private String              utf8Value;

        public BoundaryPart(IoBuffer slice)
        {
            HttpDecodeUtil.findAllHeaders(slice, this::putHeader);
            this.data = slice;
            analysisHeaders();
            mayBeUtf8Value();
        }

        public void putHeader(String header, String value)
        {
            headers.put(header, value);
        }

        public void close()
        {
            if (data != null)
            {
                data.free();
            }
        }

        private void mayBeUtf8Value()
        {
            if (!isBinary)
            {
                utf8Value = StandardCharsets.UTF_8.decode(data.readableByteBuffer()).toString();
                data.free();
                data = null;
            }
        }

        private void analysisHeaders()
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
}
