package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.common.buffer.buffer.IoBuffer;

public interface Part
{
    Part END = new Part()
    {
        @Override
        public IoBuffer originData()
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void free()
        {
            ;
        }

        @Override
        public void readEffectiveContent(IoBuffer dst)
        {
            ;
        }

        @Override
        public boolean endOfBody()
        {
            return true;
        }
    };

    IoBuffer originData();

    void free();

    /**
     * 将有效的内容部分写入到dst中
     * 注意：这个写入，不会影响到原本的data的读写位置。
     *
     * @param dst
     */
    void readEffectiveContent(IoBuffer dst);

    default boolean endOfBody()
    {
        return false;
    }
}
