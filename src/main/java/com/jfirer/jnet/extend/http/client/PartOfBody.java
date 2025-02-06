package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PartOfBody
{
    public static final PartOfBody END_OF_BODY       = new PartOfBody(3, null, 0, 0);
    public static final PartOfBody TERMINATE_OF_BODY = new PartOfBody(4, null, 0, 0);
    /**
     * 1：代表这是一个完整的，明确总长度的消息体的任意部分。
     * 2：代表这是一个不明确长度，即响应的类型是 Transfer-Encoding：chunked 的消息体的一个完整 Chunk。
     * 3：代表这是一个表达消息体已经结束的特定对象。该对象本身没有消息体数据。
     * 4：代表这是一个表达消息体因为异常被终止的特地对象。该对象本身没有消息体数据。
     * 一个 Http 响应的带数据的消息体的类型只会全部是 1 或者 2.
     */
    private final       int        type;
    private final       IoBuffer   originData;
    /**
     * 在类型 2 的情况下，完整 chunk 的头行的长度（包含头行尾部的/r/n）
     */
    private final       int        chunkHeaderLength;
    /**
     * 在类型 2 的情况下，完整 chunk 的内容体的长度（不包含代表 chunk 结尾的/r/n）
     */
    private final       int        chunkSize;

    /**
     * 获取该部分字节流中的有效部分。该操作是一次性的，对于一个 part 而言，该方法只能被调用一次。
     * 有效部分的定义：
     * 1、如果响应的类型是定长的，则整个原始数据都是有效部分。
     * 2、如果响应的类型是 chunked 编码的，则取出这个 chunk 中代表数据的部分。
     *
     * @return
     */
    public IoBuffer getEffectiveContent()
    {
        if (type == 1)
        {
            return originData;
        }
        else if (type == 2)
        {
            originData.addReadPosi(chunkHeaderLength);
            originData.addWritePosi(-2);
            return originData;
        }
        else
        {
            return null;
        }
    }

    public IoBuffer getFullOriginData()
    {
        if (type < 3)
        {
            return originData;
        }
        else
        {
            return null;
        }
    }

    public void freeBuffer()
    {
        if (type < 3)
        {
            originData.free();
        }
    }

    public boolean isEndOrTerminateOfBody()
    {
        return type > 2;
    }
}
