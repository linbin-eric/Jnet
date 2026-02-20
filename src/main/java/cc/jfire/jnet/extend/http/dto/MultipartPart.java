package cc.jfire.jnet.extend.http.dto;

import lombok.Data;

@Data
public class MultipartPart
{
    private String name;
    private String filename;
    private String contentType;
    private byte[] content;

    /**
     * 创建文本字段
     */
    public static MultipartPart text(String name, String value)
    {
        MultipartPart part = new MultipartPart();
        part.setName(name);
        part.setContent(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return part;
    }

    /**
     * 创建文件字段
     */
    public static MultipartPart file(String name, String filename, String contentType, byte[] content)
    {
        MultipartPart part = new MultipartPart();
        part.setName(name);
        part.setFilename(filename);
        part.setContentType(contentType != null ? contentType : "application/octet-stream");
        part.setContent(content);
        return part;
    }

    /**
     * 创建文件字段（默认 contentType 为 application/octet-stream）
     */
    public static MultipartPart file(String name, String filename, byte[] content)
    {
        return file(name, filename, null, content);
    }

    public boolean isFile()
    {
        return filename != null;
    }
}
