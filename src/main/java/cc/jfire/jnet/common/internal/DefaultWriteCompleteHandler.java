package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.exception.EndOfStreamException;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

@Slf4j
public class DefaultWriteCompleteHandler extends AbstractWriteCompleteHandler implements CompletionHandler<Integer, ByteBuffer>
{
    private IoBuffer sendingData;

    public DefaultWriteCompleteHandler(Pipeline pipeline)
    {
        super(pipeline);
    }

    @Override
    public void completed(Integer result, ByteBuffer byteBuffer)
    {
        try
        {
            if (byteBuffer.hasRemaining())
            {
                socketChannel.write(byteBuffer, byteBuffer, this);
                return;
            }
            writeListener.partWriteFinish(sendingData.remainRead());
            sendingData.clear();
            if (!queue.isEmpty())
            {
                writeQueuedBuffer();
                return;
            }
            for (int spin = 0; spin < SPIN_THRESHOLD; spin += 1)
            {
                if (!queue.isEmpty())
                {
                    writeQueuedBuffer();
                    return;
                }
            }
            sendingData.free();
            sendingData = null;
            int now = state;
            switch (now)
            {
                case OPEN_WORK -> quitToIdle();
                case NOTICE_WORK ->
                {
                    if (queue.isEmpty())
                    {
                        closeChannel(new EndOfStreamException());
                        quitToIdle();
                    }
                    else
                    {
                        writeQueuedBuffer();
                    }
                }
            }
        }
        catch (Throwable e)
        {
            failed(e, byteBuffer);
        }
    }

    /**
     * 从MPSCQueue中取得IoBuffer，并且执行写操作
     */
    @Override
    protected void writeQueuedBuffer()
    {
        try
        {
            int      count = 0;
            IoBuffer buffer;
            while (count < maxWriteBytes && (buffer = queue.poll()) != null)
            {
                count += buffer.remainRead();
                if (sendingData == null)
                {
                    sendingData = buffer;
                }
                else
                {
                    sendingData.put(buffer);
                    buffer.free();
                }
            }
            ByteBuffer byteBuffer = sendingData.readableByteBuffer();
            socketChannel.write(byteBuffer, byteBuffer, this);
        }
        catch (Throwable e)
        {
            failed(e, null);
        }
    }

    @Override
    public void failed(Throwable e, ByteBuffer byteBuffer)
    {
        if (sendingData != null)
        {
            sendingData.free();
            sendingData = null;
        }
        IoBuffer tmp;
        while ((tmp = queue.poll()) != null)
        {
            tmp.free();
        }
        writeListener.writeFailed(e);
        closeChannel(e);
        quitToIdle();
    }
}
