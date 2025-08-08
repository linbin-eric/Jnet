package com.jfirer.jnet.extend.http.coder;

import com.jfirer.baseutil.STR;
import com.jfirer.jnet.common.api.ReadProcessorNode;
import com.jfirer.jnet.common.buffer.buffer.IoBuffer;
import com.jfirer.jnet.common.coder.AbstractDecoder;
import com.jfirer.jnet.common.util.DataIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;

@Slf4j
@Data
public class SSLDecoder extends AbstractDecoder
{
    private final SSLEngine  sslEngine;
    private final SSLEncoder sslEncoder;
    private       boolean    handshakeFinished = false;
    private       int        count             = 1;
    private       boolean    closeInbound      = false;
    private       String     remote;

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
                log.debug("当前连接:{},当前步骤:{},当前状态:{}.当前可以读取的内容长度:{}", remote, count++, hs, accumulation.remainRead());
                IoBuffer dst = next.pipeline().allocator().allocate(sslEngine.getSession().getApplicationBufferSize());
                try
                {
                    SSLEngineResult result        = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                    int             bytesConsumed = result.bytesConsumed();
                    int             bytesProduced = result.bytesProduced();
                    accumulation.addReadPosi(bytesConsumed);
                    dst.addWritePosi(bytesProduced);
                    if (bytesProduced > 0)
                    {
                        log.warn("出现早期数据");
                    }
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        dst.free();
                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap成功，后续阶段为:{}.剩余可以读取的长度是:{}", remote, count++, hs, accumulation.remainRead());
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        dst.free();
                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap失败，因为输出空间不足", remote, count++);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        dst.free();
                        accumulation.compact();
                        if (accumulation.remainWrite() < 1000)
                        {
                            accumulation.capacityReadyFor(accumulation.capacity() * 2);
                        }
                        log.debug("当前连接:{},当前步骤:{},握手阶段unwrap失败，因为输入空间不足，读取更多数据后继续尝试。本次消耗了数据:{}.当前剩余读取大小:{}", remote, count++, result.bytesConsumed(), accumulation.remainRead());
                        return;
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
                        dst.free();
//                        if (closeInbound == false)
//                        {
//                            closeInbound = true;
//                            sslEngine.closeInbound();
//                            sslEngine.closeOutbound();
//                            sslEncoder.setSslEngine(sslEngine);
//                            next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
//                        }
//                        log.error("当前连接:{},当前步骤:{},握手阶段unwrap失败，状态为:{},因为ssl已关闭，握手结束。", remote, count, hs);
                        log.error("在握手阶段不应该出现close的状态，严重异常，系统退出");
                        System.exit(10);
                        return;
                    }
                }
                catch (SSLException e)
                {
                    log.error("当前连接:{},当前步骤:{},握手阶段unwrap失败，状态为:{},因为ssl异常，握手失败。后续状态为:{}.SSL错误原因:{}", remote, count, hs, sslEngine.getHandshakeStatus(), e.getMessage());
                    dst.free();
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
                    if (bytesConsumed > 0)
                    {
                        log.error("当前连接:{},当前步骤:{},当前状态:{},wrap时消耗了数据:{}", remote, count++, hs, bytesConsumed);
                    }
                    dst.addWritePosi(bytesProduced);
                    log.debug("当前连接:{},当前步骤:{},本次wrap，消耗了读取长度:{}，产生的写出长度:{}", remote, count++, bytesConsumed, bytesProduced);
                    SSLEngineResult.Status status = result.getStatus();
                    hs = result.getHandshakeStatus();
                    if (status == SSLEngineResult.Status.OK)
                    {
                        AsynchronousSocketChannel asynchronousSocketChannel = next.pipeline().socketChannel();
                        ByteBuffer                byteBuffer                = dst.readableByteBuffer();
                        while (byteBuffer.hasRemaining())
                        {
                            asynchronousSocketChannel.write(byteBuffer);
                        }
                        dst.free();
                        dst = null;
                        log.debug("当前连接:{},当前步骤:{},握手阶段wrap成功,后续阶段为:{}", remote, count++, hs);
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                    {
                        log.warn("当前连接:{},当前步骤:{},握手阶段wrap失败，因为输出空间不足，扩容后继续。", remote, count);
                        dst.free();
                        dst = null;
                    }
                    else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                    {
                        log.error("当前连接:{},当前步骤:{},握手阶段wrap失败，因为输入空间不足，继续尝试。", remote, count);
                        dst.free();
                        dst = null;
                        log.error("握手的输出阶段不应该出现这种情况才对");
                        System.exit(3);
                        return;
                    }
                    else if (status == SSLEngineResult.Status.CLOSED)
                    {
                        log.error("在握手阶段不应该出现close的状态，严重异常，系统退出");
                        System.exit(10);
                        return;
                    }
                }
                catch (SSLException e)
                {
                    dst.free();
                    log.error("当前连接:{},当前步骤:{},握手阶段wrap失败，状态为:{},因为ssl异常，握手失败。当前状态为:{},后续状态为:{}", remote, count, hs, sslEngine.getHandshakeStatus(), e);
                    next.pipeline().shutdownInput();
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
                log.debug("当前连接:{},当前步骤:{},握手阶段需要执行任务，任务完成。当前状态为:{}", remote, count++, hs);
            }
            else if (hs == SSLEngineResult.HandshakeStatus.FINISHED)
            {
                log.debug("当前连接:{},当前步骤:{},握手成功，开始处理数据。当前状态:{}", remote, count++, hs);
                handshakeFinished = true;
                sslEncoder.setSslEngine(sslEngine);
                log.debug("当前连接:{},当前步骤:{},握手成功，开始处理数据。当前剩余数据长度:{}", remote, count++, accumulation.remainRead());
                handData(next);
                return;
            }
            else if (hs == SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING)
            {
                log.error("严重异常");
                System.exit(100);
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
                log.debug("当前连接:{},当前步骤:{},数据处理阶段开始。当前长度为:{},当前状态:{}", remote, count++, accumulation.remainRead(), sslEngine.getHandshakeStatus());
                SSLEngineResult        result = sslEngine.unwrap(accumulation.readableByteBuffer(), dst.writableByteBuffer());
                SSLEngineResult.Status status = result.getStatus();
                log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap开始，消耗长度为:{}，产生长度为:{}", remote, count++, result.bytesConsumed(), result.bytesProduced());
                if (status == SSLEngineResult.Status.OK)
                {
                    accumulation.addReadPosi(result.bytesConsumed());
                    dst.addWritePosi(result.bytesProduced());
                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，消耗数据长度:{},当前状态:{}", remote, count++, result.bytesConsumed(), result.getHandshakeStatus());
                    next.fireRead(dst);
                    dst = null;
                    if (accumulation.remainRead() > 0)
                    {
                        log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，还有剩余数据:{}可以处理，继续。当前状态:{}", remote, count++, accumulation.remainRead(), result.getHandshakeStatus());
                        //还有数据可以处理，申请下一个空间
                        ;
                    }
                    else
                    {
                        accumulation.free();
                        accumulation = null;
                        log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap成功，数据处理结束。", remote, count++);
                        return;
                    }
                }
                else if (status == SSLEngineResult.Status.BUFFER_OVERFLOW)
                {
                    SSLSession session = sslEngine.getSession();
                    int        need    = Math.max(session.getApplicationBufferSize(), session.getPacketBufferSize()) - dst.remainWrite();
                    if (need > 0)
                    {
                        dst.capacityReadyFor(dst.capacity() + need);
                    }
                    log.warn("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为输出空间不足，扩容后继续。", remote, count++);
                }
                else if (status == SSLEngineResult.Status.BUFFER_UNDERFLOW)
                {
                    accumulation.compact();
                    if (accumulation.capacity() < sslEngine.getSession().getPacketBufferSize())
                    {
                        accumulation.capacityReadyFor(sslEngine.getSession().getPacketBufferSize());
                    }
                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为输入空间不足，继续。", remote, count++);
                    dst.free();
                    dst = null;
                    return;
                }
                else if (status == SSLEngineResult.Status.CLOSED)
                {
                    log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为ssl已关闭，握手结束。", remote, count++);
                    accumulation.addReadPosi(result.bytesConsumed());
                    dst.free();
                    dst = null;
                    if (closeInbound == false)
                    {
                        sslEngine.closeInbound();
                        sslEngine.closeOutbound();
                        closeInbound = true;
                        sslEncoder.setSslEngine(sslEngine);
                        next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
                    }
                    return;
                }
            }
        }
        catch (SSLException e)
        {
            log.debug("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为ssl异常，握手结束。", remote, count);
            throw new RuntimeException(STR.format("当前连接:{},当前步骤:{},数据处理阶段unwrap失败，因为ssl异常，握手结束。", remote, count), e);
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        try
        {
            sslEngine.closeInbound();
            sslEngine.closeOutbound();
            closeInbound = true;
//            sslEncoder.setSslEngine(sslEngine);
//            next.pipeline().fireWrite(SSLCloseNotify.INSTANCE);
        }
        catch (SSLException ex)
        {
            ;
        }
        next.pipeline().shutdownInput();
        super.readFailed(e, next);
    }

    public static class SSLCloseNotify implements DataIgnore
    {
        public static SSLCloseNotify INSTANCE = new SSLCloseNotify();
    }
}
