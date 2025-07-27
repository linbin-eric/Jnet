package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class ChunkedPart implements Part
{
    /**
     * 完整 chunk 的头行的长度（包含头行尾部的/r/n）
     */
    private final int      chunkHeaderLength;
    /**
     * 完整 chunk 的内容体的长度（不包含代表 chunk 结尾的/r/n）
     */
    private final int      chunkSize;
    private final IoBuffer originData;

    @Override
    public IoBuffer originData()
    {
        return originData;
    }

    @Override
    public void free()
    {
        originData.free();
    }

    @Override
    public void readEffectiveContent(IoBuffer dst)
    {
        int readPosi = originData.getReadPosi();
        originData.addReadPosi(chunkHeaderLength);
        dst.put(originData, chunkSize);
        originData.setReadPosi(readPosi);
    }
}
