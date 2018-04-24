package com.jfireframework.jnet.common.buffer;

import java.nio.ByteBuffer;

public interface IoBuffer
{
	void initialize(int offset, int capacity, Object memory, int index, Chunk chunk);
	
	/**
	 * 增大buffer的容量到newSize
	 * 
	 * @param newSize
	 */
	IoBuffer grow(int newSize);
	
	Chunk belong();
	
	void release();
	
	void destory();
	
	String toString();
	
	int getIndex();
	
	int capacity();
	
	IoBuffer put(byte b);
	
	IoBuffer put(byte b, int posi);
	
	IoBuffer put(byte[] content);
	
	IoBuffer put(byte[] content, int off, int len);
	
	IoBuffer put(IoBuffer bucket);
	
	IoBuffer put(IoBuffer handler, int len);
	
	IoBuffer writeInt(int i, int off);
	
	IoBuffer writeShort(short s, int off);
	
	IoBuffer writeLong(long l, int off);
	
	IoBuffer writeInt(int i);
	
	IoBuffer writeShort(short s);
	
	IoBuffer writeLong(long l);
	
	/**
	 * 返回读取位置，该读取位置的初始值为0
	 * 
	 * @return
	 */
	int getReadPosi();
	
	void setReadPosi(int readPosi);
	
	/**
	 * 返回写入位置，该写入位置的初始值为0
	 * 
	 * @return
	 */
	int getWritePosi();
	
	void setWritePosi(int writePosi);
	
	IoBuffer clearData();
	
	byte get();
	
	byte get(int posi);
	
	int remainRead();
	
	int remainWrite();
	
	IoBuffer compact();
	
	IoBuffer get(byte[] content);
	
	IoBuffer get(byte[] content, int off, int len);
	
	void addReadPosi(int add);
	
	void addWritePosi(int add);
	
	int indexOf(byte[] array);
	
	int readInt();
	
	short readShort();
	
	long readLong();
	
	/**
	 * 返回一个处于读状态的ByteBuffer。其内容为当前IoBuffer的内容
	 * 
	 * @return
	 */
	ByteBuffer byteBuffer();
	
	boolean isDirect();
	
}
