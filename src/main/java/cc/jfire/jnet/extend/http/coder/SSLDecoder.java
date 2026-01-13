package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.DataIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;

@Data
public class SSLDecoder extends AbstractDecoder
{
    private final    SSLEngine sslEngine;
    private          boolean   handshakeFinished = false;
    private          int       count             = 1;
    private volatile boolean   closeInbound      = false;
    private          String    remote;

    @Override
    protected void process0(ReadProcessorNode next)
    {
        if (remote == null)
        {
            remote = next.pipeline().getRemoteAddressWithoutException();
        }
        if (handshakeFinished)
        {
            handData(next);
        }
        else
        {
            handshake(next, null);
        }
    }

    public synchronized void gracefulClose(Pipeline pipeline)
    {
        if (closeInbound)
        {
            return;
        }
        closeInbound = true;
        try
        {
            sslEngine.closeInbound();
        }
        catch (SSLException e)
        {
//            log.error("当前连接:{}已经结束", remote, e);
        }
        sslEngine.closeOutbound();
        pipeline.fireWrite(new SSLProtocol().setCloseNotify(true));
        pipeline.shutdownInput();
    }

    private void handshake(ReadProcessorNode next, SSLEngineResult.HandshakeStatus hs)
    {
        while (true)
        {
            if (hs == null)
            {
                hs = sslEngine.getHandshakeStatus();
            }
            if (hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP || hs == SSLEngineResult.HandshakeStatus.NEED_UNWRAP_AGAIN)
            {
                if (accumulation == null || accumulation.remainRead() == 0)
                {
                    return;
                }
//                log.debug("当前连接:{},当前步骤:{},当前状态:{}.当前可以读取的内容长度:{}", remote, count++, hs, accumulation.remainRead());
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result        = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                    int             bytesConsumed = result.bytesConsumed();
                    accumulation.addReadPosi(bytesConsumed);
                    //握手阶段进行解密操作，数据不需要保留
                    dst.free();
                    dst = null;
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
//                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap成功，后续阶段为:{}.剩余可以读取的长度是:{}", remote, count++, hs, accumulation.remainRead());
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
//                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap失败，因为输出空间不足，会进行重试。下一个步骤:{}", remote, count++, hs);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        accumulation.compact();
                        if (accumulation.remainWrite() < 1000)
                        {
                            accumulation.capacityReadyFor(accumulation.capacity() * 2);
                        }
//                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap失败，因为输入空间不足，读取更多数据后继续尝试。本次消耗了数据:{}.当前剩余读取大小:{}", remote, count++, result.bytesConsumed(), accumulation.remainRead());
                        return;
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
//                        log.warn("当前连接:{},当前步骤:{}在握手协议阶段，对端发送了SSL关闭消息。现在准备关闭整条连接。", remote, count);
                        gracefulClose(next.pipeline());
                        return;
                    }
                }
                catch (SSLException e)
                {
//                    log.error("当前连接:{},当前步骤:{},握手阶段unwrap失败，状态为:{},因为ssl异常，握手失败。后续状态为:{}。SSL错误原因:{}。", remote, count++, hs, sslEngine.getHandshakeStatus(), e.getMessage(), e);
                    if (dst != null)
                    {
                        dst.free();
                        dst = null;
                    }
                    //出现致命错误，不需要优雅关闭
                    next.pipeline().shutdownInput();
                    return;
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_WRAP)
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getPacketBufferSize());
                try
                {
                    SSLEngineResult result        = sslEngine.wrap(ByteBuffer.allocate(0), dst.writableByteBuffer());
                    int             bytesProduced = result.bytesProduced();
                    int             bytesConsumed = result.bytesConsumed();
                    dst.addWritePosi(bytesProduced);
                    if (bytesConsumed > 0)
                    {
//                        log.error("当前连接:{},当前步骤:{},当前状态:{},wrap时消耗了数据:{}", remote, count++, hs, bytesConsumed);
                    }
                    if (bytesProduced > 0)
                    {
                        next.pipeline().fireWrite(new SSLProtocol().setData(dst));
                    }
                    else
                    {
                        dst.free();
                    }
                    dst = null;
//                    log.debug("当前连接:{},当前步骤:{},本次wrap，消耗了读取长度:{}，产生的写出长度:{}", remote, count++, bytesConsumed, bytesProduced);
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
//                        log.debug("当前连接:{},当前步骤:{},握手阶段wrap成功,后续阶段为:{}", remote, count++, hs);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
//                        log.warn("当前连接:{},当前步骤:{},握手阶段wrap失败，因为输出空间不足，扩容后继续。", remote, count);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
//                        log.error("握手的输出阶段不应该出现这种情况才对");
                        System.exit(3);
                        return;
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
//                        log.warn("当前连接:{},当前步骤:{}在握手协议阶段，对端发送了SSL关闭消息。现在准备关闭整条连接。", remote, count);
                        gracefulClose(next.pipeline());
                        return;
                    }
                }
                catch (SSLException e)
                {
                    if (dst != null)
                    {
                        dst.free();
                        dst = null;
                    }
//                    log.error("当前连接:{},当前步骤:{},握手阶段wrap失败，状态为:{},因为ssl异常，握手失败。当前状态为:{},后续状态为:{}", remote, count, hs, sslEngine.getHandshakeStatus(), e);
                    //出现致命错误，不需要优雅关闭
                    next.pipeline().shutdownInput();
                    return;
                }
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NEED_TASK)
            {
                Runnable task;
                while ((task = sslEngine.getDelegatedTask()) != null)
                {
                    task.run();
                }
                hs = sslEngine.getHandshakeStatus();
//                log.debug("当前连接:{},当前步骤:{},握手阶段需要执行任务，任务完成。当前状态为:{}", remote, count++, hs);
            }
            else if (hs == SSLEngineResult.HandshakeStatus.FINISHED)
            {
//                log.debug("当前连接:{},当前步骤:{},握手成功，开始处理数据。当前状态:{}", remote, count++, hs);
                handshakeFinished = true;
//                log.debug("当前连接:{},当前步骤:{},握手成功，开始处理数据。当前剩余数据长度:{}", remote, count++, accumulation.remainRead());
                handData(next);
                return;
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            {
//                log.debug("当前连接:{},还在握手期间，却退出了握手，说明本次连接终止，当前步骤:{}", remote, count++);
                gracefulClose(next.pipeline());
                return;
            }
        }
    }

    private void handData(ReadProcessorNode next)
    {
        if (accumulation.remainRead() == 0)
        {
            accumulation.free();
            accumulation = null;
            return;
        }
        try
        {
            while (true)
            {
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
//                log.debug("当前连接:{},当前步骤:{},数据处理阶段开始。当前长度为:{},当前状态:{}", remote, count++, accumulation.remainRead(), sslEngine.getHandshakeStatus());
                SSLEngineResult        result        = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                SSLEngineResult.Status status        = result.getStatus();
                int                    bytesProduced = result.bytesProduced();
                accumulation.addReadPosi(result.bytesConsumed());
                dst.addWritePosi(bytesProduced);
                if (bytesProduced > 0)
                {
                    next.fireRead(dst);
                }
                else
                {
                    dst.free();
                }
                dst = null;
//                log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap开始，消耗长度为:{}，产生长度为:{}", remote, count++, result.bytesConsumed(), bytesProduced);
                if (status == SSLEngineResult.Status.OK)
                {
//                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，消耗数据长度:{},下一步骤:{}", remote, count++, result.bytesConsumed(), result.getStatus());
                    if (accumulation.remainRead() > 0)
                    {
//                        log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，还有剩余数据:{}可以处理，继续。下一步骤:{}", remote, count++, accumulation.remainRead(), result.getStatus());
                        //还有数据可以处理，申请下一个空间
                        ;
                    }
                    else
                    {
                        accumulation.free();
                        accumulation = null;
//                        log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，数据处理结束。", remote, count++);
                        return;
                    }
                }
                else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                {
//                    log.warn("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为输出空间不足，扩容后继续。", remote, count++);
                }
                else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                {
                    accumulation.compact();
                    if (accumulation.capacity() < sslEngine.getSession().getPacketBufferSize())
                    {
                        accumulation.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
                    }
//                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为输入空间不足，继续。", remote, count++);
                    return;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
//                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为ssl已关闭，握手结束。", remote, count++);
                    gracefulClose(next.pipeline());
                    return;
                }
            }
        }
        catch (SSLException e)
        {
//            log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为ssl异常，握手结束。", remote, count);
            //出现致命错误，不需要优雅关闭
            if (accumulation != null)
            {
                accumulation.free();
                accumulation = null;
            }
            next.pipeline().shutdownInput();
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
        super.readFailed(e, next);
    }

    @Data
    @Accessors(chain = true)
    public static class SSLProtocol implements DataIgnore
    {
        /**
         * 当为true的时候，意味着是关闭ssl连接，需要发送端生成close_notify消息。
         * 当为false的时候，意味着有数据需要发出
         */
        private boolean  closeNotify = false;
        private IoBuffer data;
    }
}
