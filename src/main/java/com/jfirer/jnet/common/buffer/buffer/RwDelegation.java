package com.jfirer.jnet.common.buffer.buffer;

import java.nio.ByteBuffer;

public interface RwDelegation
{
    void put0(int posi, byte value, Object memory, int offset, long nativeAddress);

    void put0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress);

    void putInt0(int posi, int value, Object memory, int offset, long nativeAddress);

    void putShort0(int posi, short value, Object memory, int offset, long nativeAddress);

    void putLong0(int posi, long value, Object memory, int offset, long nativeAddress);

    byte get0(int posi, Object memory, int offset, long nativeAddress);

    void get0(byte[] content, int off, int len, int posi, Object memory, int memoryOffset, long nativeAddress);

    int getInt0(int posi, Object memory, int offset, long nativeAddress);

    short getShort0(int posi, Object memory, int offset, long nativeAddress);

    long getLong0(int posi, Object memory, int offset, long nativeAddress);

    ByteBuffer writableByteBuffer(Object memory, int offset, long nativeAddress, int writePosi, int capacity);

    ByteBuffer readableByteBuffer(Object memory, int offset, long nativeAddress, int readPosition, int writePosition);

    void compact0(Object memory, int offset, long nativeAddress, int readPosition, int length);

    void put(Object destMemory, int destOffset, long destNativeAddress, int destPosi, IoBuffer srcBuf, int len);
}
