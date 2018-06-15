package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.jnet.common.buffer.Bits;
import com.jfireframework.jnet.common.buffer.Chunk;
import com.jfireframework.jnet.common.buffer.PooledIoBuffer;

@RunWith(Parameterized.class)
public class BufferReadWriteTest
{
	
	@Parameters
	public static Collection<?> data()
	{
		return Arrays.asList(new Object[][] { //
		        { Chunk.newHeapChunk(2, 128), PooledIoBuffer.heapBuffer(), true }, //
		        { Chunk.newDirectChunk(2, 128), PooledIoBuffer.directBuffer(), true }, //
		        { Chunk.newHeapChunk(2, 128), PooledIoBuffer.heapBuffer(), false }, //
		        { Chunk.newDirectChunk(2, 128), PooledIoBuffer.directBuffer(), false }//
		});
	}
	
	private Chunk			chunk;
	private PooledIoBuffer	buffer;
	
	public BufferReadWriteTest(Chunk chunk, PooledIoBuffer buffer, boolean unaliagned)
	{
		this.chunk = chunk;
		this.buffer = buffer;
		Bits.unaligned = unaliagned;
	}
	
	@Test
	public void test()
	{
		chunk.apply(50, buffer, false);
		buffer.put((byte) 27);
		buffer.addWritePosi(1);
		buffer.put(new byte[] { 36, 90 });
		buffer.addWritePosi(1);
		buffer.putInt(5);
		buffer.putLong(12564l);
		buffer.putShort((short) 1000);
		assertEquals((byte) 27, buffer.get());
		buffer.addReadPosi(1);
		assertEquals((byte) 36, buffer.get());
		assertEquals((byte) 90, buffer.get());
		buffer.addReadPosi(1);
		assertEquals(5, buffer.getInt());
		assertEquals(12564l, buffer.getLong());
		assertEquals((short) 1000, buffer.getShort());
	}
}
