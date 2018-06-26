package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.jnet.common.buffer2.Chunk;
import com.jfireframework.jnet.common.buffer2.DirectChunk;
import com.jfireframework.jnet.common.buffer2.HeapChunk;
import com.jfireframework.jnet.common.util.MathUtil;

@RunWith(Parameterized.class)
public class ChunkTest
{
	private Chunk<?>	chunk;
	private Chunk<?>	chunk2;
	
	public ChunkTest(Chunk<?> chunk, Chunk<?> chunk2)
	{
		this.chunk = chunk;
		this.chunk2 = chunk2;
	}
	
	@Parameters
	public static Collection<?> param()
	{
		return Arrays.asList(new Object[][] { //
		        { new HeapChunk(4, 1), new HeapChunk(4, 128) }, //
		        { new DirectChunk(4, 1), new DirectChunk(4, 128) },//
		});
	}
	
	@Test
	public void test()
	{
		assertEquals(16, chunk.allocate(MathUtil.tableSizeFor(1)));
		assertEquals(9, chunk.allocate(MathUtil.tableSizeFor(2)));
		assertEquals(10, chunk.allocate(MathUtil.tableSizeFor(2)));
		assertEquals(6, chunk.allocate(MathUtil.tableSizeFor(4)));
		assertEquals(-1, chunk.allocate(MathUtil.tableSizeFor(8)));
		assertEquals(11, chunk.allocate(MathUtil.tableSizeFor(2)));
		assertEquals(14, chunk.allocate(MathUtil.tableSizeFor(2)));
		assertEquals(17, chunk.allocate(MathUtil.tableSizeFor(1)));
	}
	
	@Test
	public void test2()
	{
		long index1 = chunk2.allocate(MathUtil.tableSizeFor(400));
		assertEquals(4, index1);
		long handle2 = chunk2.allocate(MathUtil.tableSizeFor(100));
		assertEquals(20, handle2);
		long handle3 = chunk2.allocate(MathUtil.tableSizeFor(200));
		assertEquals(11, handle3);
		long handle4 = chunk2.allocate(MathUtil.tableSizeFor(100));
		assertEquals(21, handle4);
		long handle5 = chunk2.allocate(MathUtil.tableSizeFor(500));
		assertEquals(6, handle5);
		chunk2.free(handle2);
		chunk2.free(handle3);
		chunk2.free(handle4);
		long handle6 = chunk2.allocate(MathUtil.tableSizeFor(400));
		assertEquals(5, handle6);
	}
}
