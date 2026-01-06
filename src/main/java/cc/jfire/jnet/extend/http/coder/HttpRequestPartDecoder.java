package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpRequestChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;

@Slf4j
public class HttpRequestPartDecoder extends AbstractDecoder
{
    private static final int                 MAX_CHUNK_SIZE_LINE_LENGTH = 32;
    private              int                 lastCheck                  = -1;
    private              ParseState          state                      = ParseState.REQUEST_LINE;
    private              HttpRequestPartHead reqHead;
    private              int                 headStartPosi              = -1;
    private              long                bodyRead                   = 0;
    private              int                 chunkSize                  = -1;
    private              int                 chunkSizeLineLength        = -1;

    @Override
    protected void process0(ReadProcessorNode next)
    {
//        log.trace("[HttpRequestPartDecoder] process0 开始, 当前状态: {}, accumulation剩余: {}",
//                  state, accumulation != null ? accumulation.remainRead() : 0);
        boolean goToNextState;
        do
        {
            goToNextState = switch (state)
            {
                case REQUEST_LINE -> parseRequestLine();
                case REQUEST_HEADER -> parseRequestHeader(next);
                case BODY_FIX_LENGTH -> parseBodyFixLength(next);
                case BODY_CHUNKED -> parseBodyChunked(next);
                case NO_BODY ->
                {
                    // NO_BODY 状态已在 parseRequestHeader 中处理并重置，此分支不应被执行
                    resetState();
                    yield accumulation != null && accumulation.remainRead() > 0;
                }
            };
        } while (goToNextState);
    }

    private boolean parseRequestLine()
    {
//        log.trace("[HttpRequestPartDecoder] parseRequestLine");
        if (lastCheck == -1)
        {
            lastCheck = accumulation.getReadPosi();
            headStartPosi = lastCheck;
        }
        for (; lastCheck + 1 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n')
            {
                lastCheck += 2;
                state = ParseState.REQUEST_HEADER;
                break;
            }
        }
        if (state == ParseState.REQUEST_HEADER)
        {
            reqHead = new HttpRequestPartHead();
            decodeRequestLine();
//            log.trace("[HttpRequestPartDecoder] 解析请求行完成: {} {} {}",
//                      reqHead.getMethod(), reqHead.getPath(), reqHead.getVersion());
            return true;
        }
        return false;
    }

    private void decodeRequestLine()
    {
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                reqHead.setMethod(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == ' ')
            {
                reqHead.setPath(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                accumulation.setReadPosi(i + 1);
                break;
            }
        }
        for (int i = accumulation.getReadPosi(); i < lastCheck; i++)
        {
            if (accumulation.get(i) == '\r')
            {
                reqHead.setVersion(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
                break;
            }
        }
        accumulation.setReadPosi(lastCheck);
    }

    private boolean parseRequestHeader(ReadProcessorNode next)
    {
        for (; lastCheck + 3 < accumulation.getWritePosi(); lastCheck++)
        {
            if (accumulation.get(lastCheck) == '\r' && accumulation.get(lastCheck + 1) == '\n' && accumulation.get(lastCheck + 2) == '\r' && accumulation.get(lastCheck + 3) == '\n')
            {
                int headEndPosi = lastCheck + 4;
                int headLength  = headEndPosi - headStartPosi;
                lastCheck = -1;
                HttpDecodeUtil.findAllHeaders(accumulation, reqHead::addHeader);
                HttpDecodeUtil.findContentLength(reqHead.getHeaders(), reqHead::setContentLength);
                accumulation.setReadPosi(headStartPosi);
                reqHead.setHeadBuffer(accumulation.slice(headLength));
                parseBodyType();
//                log.trace("[HttpRequestPartDecoder] 解析请求头完成, Content-Length: {}, 是否chunked: {}, 状态: {}",
//                          reqHead.getContentLength(), reqHead.isChunked(), state);
                // 如果没有 body，设置 last = true
                if (state == ParseState.NO_BODY)
                {
                    reqHead.setLast(true);
//                    log.trace("[HttpRequestPartDecoder] 无请求体, 设置 last=true");
                }
//                log.trace("[HttpRequestPartDecoder] 发送 HttpRequestPartHead: {} {}", reqHead.getMethod(), reqHead.getPath());
                next.fireRead(reqHead);
                // 如果没有 body，直接重置状态，不再发送 End
                if (state == ParseState.NO_BODY)
                {
                    resetState();
                }
                return accumulation != null && accumulation.remainRead() > 0;
            }
        }
        return false;
    }

    private void parseBodyType()
    {
        if (reqHead.getContentLength() > 0)
        {
            state = ParseState.BODY_FIX_LENGTH;
        }
        else
        {
            boolean hasTransferEncoding = reqHead.getHeaders().entrySet().stream().anyMatch(e -> e.getKey().equalsIgnoreCase("Transfer-Encoding") && e.getValue().equalsIgnoreCase("chunked"));
            if (hasTransferEncoding)
            {
                state = ParseState.BODY_CHUNKED;
                reqHead.setChunked(true);
            }
            else
            {
                state = ParseState.NO_BODY;
            }
        }
    }

    private boolean parseBodyFixLength(ReadProcessorNode next)
    {
//        log.trace("[HttpRequestPartDecoder] parseBodyFixLength 开始, bodyRead: {}, contentLength: {}",
//                  bodyRead, reqHead.getContentLength());
        if (accumulation == null || accumulation.remainRead() == 0)
        {
//            log.trace("[HttpRequestPartDecoder] parseBodyFixLength 无数据可读");
            return false;
        }
        long left   = reqHead.getContentLength() - bodyRead;
        int remain = accumulation.remainRead();
//        log.trace("[HttpRequestPartDecoder] parseBodyFixLength: 剩余需读: {}, 当前可读: {}", left, remain);
        if (remain >= left)
        {
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
            part.setLast(true);
            if (remain > left)
            {
//                log.trace("当前 slice");
                part.setPart(accumulation.slice((int) left));
            }
            else
            {
//                log.trace("当前直接采用 accumulation");
                part.setPart(accumulation);
                accumulation = null;
            }
//            log.trace("[HttpRequestPartDecoder] 发送 FixLengthBodyPart, last=true, 大小: {}", left);
            next.fireRead(part);
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        else
        {
            bodyRead += remain;
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
            part.setLast(false);
            part.setPart(accumulation);
            accumulation = null;
//            log.trace("[HttpRequestPartDecoder] 发送 FixLengthBodyPart, last=false, 大小: {}, 已读总计: {}", remain, bodyRead);
            next.fireRead(part);
            return false;
        }
    }

    private boolean parseBodyChunked(ReadProcessorNode next)
    {
//        log.trace("[HttpRequestPartDecoder] parseBodyChunked 开始, chunkSize: {}", chunkSize);
        if (chunkSize == -1)
        {
            int startPosi = accumulation.getReadPosi();
            for (int i = startPosi; i < accumulation.getWritePosi() - 1; i++)
            {
                if (i - startPosi > MAX_CHUNK_SIZE_LINE_LENGTH)
                {
                    throw new IllegalStateException("Chunk size line exceeds " + MAX_CHUNK_SIZE_LINE_LENGTH + " bytes");
                }
                if (accumulation.get(i) == '\r' && accumulation.get(i + 1) == '\n')
                {
                    chunkSize           = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString().trim(), 16);
                    chunkSizeLineLength = i + 2 - startPosi;
//                    log.trace("[HttpRequestPartDecoder] 解析chunk大小: {}, 头部长度: {}", chunkSize, chunkSizeLineLength);
                    break;
                }
            }
        }
        if (chunkSize == -1)
        {
//            log.trace("[HttpRequestPartDecoder] chunk大小未解析完成");
            return false;
        }
        // 处理 chunk size 为 0 的情况（结束标志）
        if (chunkSize == 0)
        {
            int totalLength = chunkSizeLineLength + 2;
            if (accumulation.remainRead() < totalLength)
            {
//                log.trace("[HttpRequestPartDecoder] 等待最后chunk的CRLF");
                return false;
            }
            HttpRequestChunkedBodyPart part = new HttpRequestChunkedBodyPart();
            part.setHeadLength(chunkSizeLineLength);
            part.setChunkLength(totalLength);
            part.setPart(accumulation.slice(totalLength));
            part.setLast(true);
//            log.trace("[HttpRequestPartDecoder] 发送 ChunkedBodyPart, last=true (结束chunk)");
            next.fireRead(part);
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }

        // 检查是否已接收完整的 chunk（chunk size 行 + chunk 数据 + 结尾 CRLF）
        int totalChunkLength = chunkSizeLineLength + chunkSize + 2;
        if (accumulation.remainRead() < totalChunkLength)
        {
//            log.trace("[HttpRequestPartDecoder] 等待完整chunk, 需要: {}, 可用: {}", totalChunkLength, accumulation.remainRead());
            return false;
        }

        HttpRequestChunkedBodyPart part = new HttpRequestChunkedBodyPart();
        part.setHeadLength(chunkSizeLineLength);
        part.setChunkLength(totalChunkLength);
        part.setPart(accumulation.slice(totalChunkLength));
        part.setLast(false);
//        log.trace("[HttpRequestPartDecoder] 发送 ChunkedBodyPart, last=false, chunkSize: {}", chunkSize);
        next.fireRead(part);
        chunkSize           = -1;
        chunkSizeLineLength = -1;
        return true;
    }

    private void resetState()
    {
//        log.trace("[HttpRequestPartDecoder] resetState, 重置解析器状态");
        reqHead       = null;
        headStartPosi = -1;
        bodyRead      = 0;
        chunkSize = -1;
        chunkSizeLineLength = -1; // 新增：重置chunk size行长度
        state     = ParseState.REQUEST_LINE;
        if (accumulation != null)
        {
            if (accumulation.remainRead() != 0)
            {
                accumulation.compact();
            }
        }
    }

    enum ParseState
    {
        REQUEST_LINE, REQUEST_HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
