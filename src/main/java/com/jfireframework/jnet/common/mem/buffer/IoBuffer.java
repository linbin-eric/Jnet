package com.jfireframework.jnet.common.mem.buffer;

import java.nio.ByteBuffer;

/**
 * 用于对外部提供服务。具备自动扩容功能。
 * 
 * @author linbin
 *
 */
public interface IoBuffer
{
	int indexOf(byte[] array);
	
	IoBuffer maskRead();
	
	IoBuffer maskWritePosi();
	
	IoBuffer resetReadPosi();
	
	IoBuffer resetWritePosi();
	
	IoBuffer readPosi(int readPosi);
	
	IoBuffer writePosi(int writePosi);
	
	IoBuffer addReadPosi(int add);
	
	IoBuffer addWritePosi(int add);
	
	int readPosi();
	
	int writePosi();
	
	IoBuffer put(byte b);
	
	IoBuffer put(byte b, int posi);
	
	IoBuffer put(byte[] content);
	
	IoBuffer put(byte[] content, int off, int len);
	
	IoBuffer clear();
	
	byte get();
	
	byte get(int posi);
	
	int remainRead();
	
	int remainWrite();
	
	IoBuffer compact();
	
	IoBuffer get(byte[] content);
	
	IoBuffer get(byte[] content, int off, int len);
	
	IoBuffer put(IoBuffer buffer, int length);
	
	IoBuffer put(IoBuffer buffer);
	
	void release();
	
	int size();
	
	void expansion(int newSize);
	
	int readInt();
	
	short readShort();
	
	long readLong();
	
	IoBuffer writeInt(int i, int off);
	
	IoBuffer writeLong(long l, int off);
	
	IoBuffer writeShort(short s, int off);
	
	IoBuffer writeInt(int i);
	
	IoBuffer writeLong(long l);
	
	IoBuffer writeShort(short s);
	
	/**
	 * 返回一个可读状态的ByteBuffer
	 * 
	 * @return
	 */
	ByteBuffer byteBuffer();
	
	ByteBuffer cachedByteBuffer();
}
