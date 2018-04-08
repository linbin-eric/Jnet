package com.jfireframework.jnet.common.mem.handler;

import java.nio.ByteBuffer;
import com.jfireframework.jnet.common.mem.chunk.Chunk;

/**
 * 承载从Chunk处获取的内存区域。并且保存了本次获取的相关属性
 * 
 * @author linbin
 *
 * @param <T>
 */
public interface Handler<T>
{
	/**
	 * 将可用内存初始化到handler中。
	 * 
	 * @param off mem中可用的起始偏移量
	 * @param len 可用mem的字节长度
	 * @param mem 内存
	 * @param level 申请的这段内存在chunk中的level
	 * @param index 申请的这段内存在chunk中的index
	 * @param chunkMem 托管这段内存的chunk
	 */
	void initialize(int off, int len, T mem, int index, Chunk<T> chunkMem);
	
	/**
	 * 销毁所有数据
	 */
	void destory();
	
	Chunk<T> belong();
	
	int getIndex();
	
	int capacity();
	
	/**
	 * 获取读取位置，该位置是相对位置。默认起始为0
	 * 
	 * @return
	 */
	int getReadPosi();
	
	/**
	 * 设置读取位置，该位置是相对位置，默认起始为0
	 * 
	 * @param readPosi
	 */
	void setReadPosi(int readPosi);
	
	/**
	 * 读取写入位置，该位置是相对位置，默认起始为0
	 * 
	 * @return
	 */
	int getWritePosi();
	
	/**
	 * 设置写入位置，该位置是相对位置，默认起始为0
	 * 
	 * @param writePosi
	 */
	void setWritePosi(int writePosi);
	
	Handler<T> put(byte b);
	
	Handler<T> put(byte b, int posi);
	
	Handler<T> put(byte[] content);
	
	Handler<T> put(byte[] content, int off, int len);
	
	Handler<T> clear();
	
	byte get();
	
	byte get(int posi);
	
	int remainRead();
	
	int remainWrite();
	
	Handler<T> compact();
	
	Handler<T> get(byte[] content);
	
	Handler<T> get(byte[] content, int off, int len);
	
	/**
	 * 将一个handler的数据放入本handler。对入参的handler数据无影响
	 * 
	 * @param bucket
	 * @return
	 */
	Handler<T> put(Handler<?> bucket);
	
	/**
	 * 将一个handler的部分数据放入本handler。对入参的handler数据无影响
	 * 
	 * @param bucket
	 * @return
	 */
	Handler<T> put(Handler<?> bucket, int len);
	
	boolean isEnoughWrite(int size);
	
	void addReadPosi(int add);
	
	void addWritePosi(int add);
	
	/**
	 * 返回数组在数据中的起始位置。该位置为相对位置。初始为0.如果没有找到，则返回-1
	 * 
	 * @param array
	 * @return
	 */
	int indexOf(byte[] array);
	
	int readInt();
	
	short readShort();
	
	long readLong();
	
	/**
	 * 在位置off处写入int变量i。该off为相对位置
	 * 
	 * @param i
	 * @param off
	 */
	void writeInt(int i, int off);
	
	/**
	 * 在位置off处写入short变量s。该off为相对位置
	 * 
	 * @param s
	 * @param off
	 */
	void writeShort(short s, int off);
	
	/**
	 * 在位置off处写入long变量l。该off为相对位置
	 * 
	 * @param l
	 * @param off
	 */
	void writeLong(long l, int off);
	
	/**
	 * 写入int变量i
	 * 
	 * @param i
	 * @param off
	 */
	void writeInt(int i);
	
	/**
	 * 写入short变量s
	 * 
	 * @param s
	 * @param off
	 */
	void writeShort(short s);
	
	/**
	 * 写入long变量l
	 * 
	 * @param l
	 * @param off
	 */
	void writeLong(long l);
	
	ByteBuffer byteBuffer();
	
	ByteBuffer cachedByteBuffer();
}
