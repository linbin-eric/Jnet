package cc.jfire.jnet.common.internal;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.exception.EndOfStreamException;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

public class GatherWriteCompleteHandler extends AbstractWriteCompleteHandler implements CompletionHandler<Long, Void>
{
    private final int        gatherSize    = 2048;
    private final IoBuffer[] gatherBuffers = new IoBuffer[gatherSize];
    private final ByteBuffer[] gatherByteBuffers = new ByteBuffer[gatherSize];
    private       int          completedStart    = 0;
    private int          writeCount        = 0;

    public GatherWriteCompleteHandler(Pipeline pipeline)
    {
        super(pipeline);
    }

    @Override
    protected void writeQueuedBuffer()
    {
        try
        {
            int      count = 0;
            IoBuffer buffer;
            while (count < gatherSize && (buffer = queue.poll()) != null)
            {
                gatherBuffers[count]     = buffer;
                gatherByteBuffers[count] = buffer.readableByteBuffer();
                count++;
            }
            completedStart = 0;
            writeCount     = count;
            socketChannel.write(gatherByteBuffers, 0, count, Long.MAX_VALUE, TimeUnit.SECONDS, null, this);
        }
        catch (Throwable e)
        {
            failed(e, null);
        }
    }

    @Override
    public void completed(Long result, Void bufferCount)
    {
        try
        {
            for (int i = completedStart; i < writeCount; i++)
            {
                if (gatherByteBuffers[i].hasRemaining())
                {
                    completedStart = i;
                    socketChannel.write(gatherByteBuffers, i, writeCount - i, Long.MAX_VALUE, TimeUnit.SECONDS, null, this);
                    return;
                }
            }
            for (int i = 0; i < writeCount; i++)
            {
                gatherByteBuffers[i] = null;
            }
            int currentSend = 0;
            for (int i = 0; i < writeCount; i++)
            {
                currentSend += gatherBuffers[i].getWritePosi();
                gatherBuffers[i].free();
                gatherBuffers[i] = null;
            }
            writeCount = completedStart = 0;
            writeListener.partWriteFinish(currentSend);
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
            failed(e, null);
        }
    }

    @Override
    public void failed(Throwable e, Void attachment)
    {
        for (int i = 0; i < writeCount; i++)
        {
            gatherByteBuffers[i] = null;
        }
        for (int i = 0; i < writeCount; i++)
        {
            if (gatherBuffers[i] != null)
            {
                gatherBuffers[i].free();
                gatherBuffers[i] = null;
            }
        }
        writeListener.writeFailed(e);
        pipeline.fireWriteFailed(e);
        IoBuffer tmp;
        while ((tmp = queue.poll()) != null)
        {
            tmp.free();
        }
        closeChannel(e);
        quitToIdle();
    }
}
