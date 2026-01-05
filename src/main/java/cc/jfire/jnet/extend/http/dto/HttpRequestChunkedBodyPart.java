package cc.jfire.jnet.extend.http.dto;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class HttpRequestChunkedBodyPart implements HttpRequestPart
{
    /**
     * chunk 的头部长度，含 CRLF
     */
    private int      headLength;
    /**
     * chunk 的长度，含 CRLF
     */
    private int      chunkLength;
    /**
     * 完整的内容，包含头部，内容，CRLF
     */
    private IoBuffer part;
    private boolean  last = false;

    @Override
    public boolean isLast()
    {
        return last;
    }

    public void setLast(boolean last)
    {
        this.last = last;
    }

    @Override
    public void close()
    {
        if (part != null)
        {
            part.free();
            part = null;
        }
    }
}
