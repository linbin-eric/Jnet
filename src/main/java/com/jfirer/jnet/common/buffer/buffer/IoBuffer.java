package com.jfirer.jnet.common.buffer.buffer;

import com.jfirer.jnet.common.buffer.buffer.impl.AbstractBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.SliceDirectBuffer;
import com.jfirer.jnet.common.buffer.buffer.impl.SliceHeapBuffer;

import java.nio.ByteBuffer;

public interface IoBuffer<T>
{
    /**
     * 返回当前Buffer的大小，该数值并不是固定值。因为Buffer是会随着写入自动扩容
     *
     * @return
     */
    int capacity();

    /**
     * 在当前位置写入一个byte
     *
     * @param b
     * @return
     */
    IoBuffer put(byte b);

    /**
     * 在指定位置写入一个byte
     *
     * @param b
     * @param posi
     * @return
     */
    IoBuffer put(byte b, int posi);

    /**
     * 写入一个字节数组
     *
     * @param content
     * @return
     */
    IoBuffer put(byte[] content);

    /**
     * 写入一个字节数组，指定起始位置和写入长度
     *
     * @param content
     * @param off
     * @param len
     * @return
     */
    IoBuffer put(byte[] content, int off, int len);

    /**
     * 将一个buffer的内容写入该buffer。该写入不影响参数buffer的所有参数
     *
     * @param buffer
     * @return
     */
    IoBuffer put(IoBuffer buffer);

    /**
     * 将一个buffer的内容写入该buffer，并且指定写入长度。该写入不影响参数buffer的所有参数。
     *
     * @param buffer
     * @param len
     * @return
     */
    IoBuffer put(IoBuffer buffer, int len);

    /**
     * 在当前位置写入一个int
     *
     * @param i
     * @return
     */
    IoBuffer putInt(int i);

    /**
     * 在指定位置posi写入一个int
     *
     * @param value
     * @param posi
     * @return
     */
    IoBuffer putInt(int value, int posi);

    /**
     * 在指定位置posi写入一个short
     *
     * @param value
     * @param posi
     * @return
     */
    IoBuffer putShort(short value, int posi);

    /**
     * 在指定位置posi写入一个long
     *
     * @param value
     * @param posi
     * @return
     */
    IoBuffer putLong(long value, int posi);

    /**
     * 在当前位置写入一个short
     *
     * @param s
     * @return
     */
    IoBuffer putShort(short s);

    /**
     * 在当前位置写入一个long
     *
     * @param l
     * @return
     */
    IoBuffer putLong(long l);

    /**
     * 返回读取位置，该读取位置的初始值为0
     *
     * @return
     */
    int getReadPosi();

    /**
     * 设置读取位置
     *
     * @param readPosi
     */
    IoBuffer setReadPosi(int readPosi);

    /**
     * 返回写入位置，该写入位置的初始值为0
     *
     * @return
     */
    int getWritePosi();

    /**
     * 设置写入位置
     *
     * @param writePosi
     */
    IoBuffer setWritePosi(int writePosi);

    /**
     * 将写入位置和读取位置归零
     *
     * @return
     */
    IoBuffer clear();

    /**
     * 将读取位置和写入位置归零。并且将整个区域清空为0
     *
     * @return
     */
    IoBuffer clearAndErasureData();

    /**
     * 在当前读取位置读取一个byte并返回。读取位置自增。
     *
     * @return
     */
    byte get();

    /**
     * 在指定位置读取一个byte并且返回。
     *
     * @param posi
     * @return
     */
    byte get(int posi);

    /**
     * 返回剩余可以读取的字节数。
     *
     * @return
     */
    int remainRead();

    /**
     * 返回没有自动扩容前剩余的可写入字节数
     *
     * @return
     */
    int remainWrite();

    /**
     * 当前Buffer进行压缩。将剩余的读取数据拷贝至buffer的最前端。调整新读取位置为0。新写入位置为调整为旧写入位置减去移动的长度。
     *
     * @return
     */
    IoBuffer compact();

    /**
     * 从当前位置读取数据填充参数数组。读取位置增加数组长度
     *
     * @param content
     * @return
     */
    IoBuffer get(byte[] content);

    /**
     * 从当前位置读取数据填充参数数组，从参数数组的off位置开始，填充长度为len。读取位置增加len。
     *
     * @param content
     * @param off
     * @param len
     * @return
     */
    IoBuffer get(byte[] content, int off, int len);

    /**
     * 读取位置增加add
     *
     * @param add
     */
    IoBuffer addReadPosi(int add);

    /**
     * 写入位置增加add
     *
     * @param add
     */
    IoBuffer addWritePosi(int add);

    /**
     * 在数据内容中检索特定的字节数组存在。如果存在，则返回对应的读取位置。否则返回-1
     *
     * @param array
     * @return
     */
    int indexOf(byte[] array);

    /**
     * 在读取位置读取int。读取位置增加4
     *
     * @return
     */
    int getInt();

    /**
     * 在读取位置读取short。读取位置增加2
     *
     * @return
     */
    short getShort();

    /**
     * 在读取位置读取long。读取位置增加8
     *
     * @return
     */
    long getLong();

    /**
     * 在位置posi读取一个int。该操作不会影响readPosi
     *
     * @param posi
     * @return
     */
    int getInt(int posi);

    /**
     * 在位置posi读取一个short。该操作不会影响readPosi
     *
     * @param posi
     * @return
     */
    short getShort(int posi);

    /**
     * 在位置posi读取一个long。该操作不会影响readPosi
     *
     * @param posi
     * @return
     */
    long getLong(int posi);

    /**
     * 返回一个处于读模式的ByteBuffer。该ByteBuffer的可读内容为[readPosi,writePosi)之间。<br/>
     * 该ByteBuffer共享了IoBuffer的内容空间。任何对该Buffer的写入操作都会反映在IoBuffer上
     *
     * @return
     */
    ByteBuffer readableByteBuffer();

    /**
     * 返回一个处于写模式的ByteBuffer。该ByteBuffer的可写范围是[writePosi,capacity).<br/>
     * 该ByteBuffer共享了IoBuffer的内容空间。任何对该Buffer的写入操作都会反映在IoBuffer上
     *
     * @return
     */
    ByteBuffer writableByteBuffer();

    boolean isDirect();

    /**
     * 释放该Buffer持有的空间以及实例对象
     */
    void free();

    /**
     * 扩容容量，使得最少支持newCapacity
     *
     * @param newCapacity
     */
    IoBuffer capacityReadyFor(int newCapacity);

    /**
     * 从当前buffer的读取下标开始，切分出一个长度为length的新的IoBuffer。自身读取下标增加length。
     * 新的IoBuffer的容量为length，读取下标为0，写入下标为length。
     * 新的IoBuffer和原先的IoBuffer共享同一个底层存储
     *
     * @param length
     * @return
     */
    default IoBuffer slice(int length)
    {
        if (isDirect())
        {
            return SliceDirectBuffer.slice((AbstractBuffer<ByteBuffer>) this, length);
        }
        else
        {
            return SliceHeapBuffer.slice((AbstractBuffer<byte[]>) this, length);
        }
    }

    /**
     * 返回当前Buffer持有的存储区域被多少对象持有
     *
     * @return
     */
    int refCount();

    T memory();
}
