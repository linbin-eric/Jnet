package com.jfireframework.jnet.test.mem;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import com.jfireframework.jnet.common.buffer2.UnPooledBuffer;

@RunWith(Parameterized.class)
public class UnPooledBufferRWTest
{
	private UnPooledBuffer<?> buffer;
	
	@Parameters
	public static Collection<?> data()
	{
		return Arrays.asList(new Object[] { UnPooledBuffer.newHeapUnPooledBuffer(128), UnPooledBuffer.newDirectUnPooledBuffer(128)
		});
	}
	
	public UnPooledBufferRWTest(UnPooledBuffer<?> buffer)
	{
		this.buffer = buffer;
	}
	
	@Test
	public void test()
	{
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
