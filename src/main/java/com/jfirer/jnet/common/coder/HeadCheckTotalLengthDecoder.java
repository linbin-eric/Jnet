package com.jfirer.jnet.common.coder;

import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

/**
 * 报文格式：
 * 1、固定报文头
 * 2、4字节报文体长度
 * 3、报文体
 */
@Data
public class HeadCheckTotalLengthDecoder extends AbstractDecoder
{
    private final byte[] headCheck;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        do
        {
            if (accumulation.remainRead() == 0)
            {
                accumulation.free();
                accumulation = null;
                return;
            }
            if (accumulation.remainRead() < headCheck.length + 4)
            {
                accumulation.compact();
                return;
            }
            for (int i = 0; i < headCheck.length; i++)
            {
                if (accumulation.get() != headCheck[i])
                {
                    next.pipeline().shutdownInput();
                    return;
                }
            }
            int length = accumulation.getInt();
            if (length <= accumulation.remainRead())
            {
                IoBuffer packet = accumulation.slice(length);
                next.fireRead(packet);
            }
            else
            {
                accumulation.setReadPosi(accumulation.getReadPosi() - 4 - headCheck.length);
                if (accumulation.remainRead() < length / 2)
                {
                    accumulation.compact();
                }
                return;
            }
        } while (true);
    }
}
