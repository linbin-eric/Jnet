package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import lombok.Data;

@Data
public class FixLengthPart implements Part
{
    private final IoBuffer data;

    @Override
    public IoBuffer originData()
    {
        return data;
    }

    @Override
    public void free()
    {
        data.free();
    }

    @Override
    public void readEffectiveContent(IoBuffer dst)
    {
        int readPosi = data.getReadPosi();
        dst.put(data);
        data.setReadPosi(readPosi);
    }
}
