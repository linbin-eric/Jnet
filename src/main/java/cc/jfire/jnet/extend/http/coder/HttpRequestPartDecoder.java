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
        boolean goToNextState;
        do
        {
            goToNextState = switch (state)
            {
                case REQUEST_LINE -> parseRequestLine();
                case REQUEST_HEADER -> parseRequestHeader(next);
                case BODY_FIX_LENGTH -> parseBodyFixLength(next);
                case BODY_CHUNKED -> parseBodyChunked(next);
                case NO_BODY -> throw new IllegalStateException();
            };
        } while (goToNextState);
    }

    private boolean parseRequestLine()
    {
//        log.trace("[HttpRequestPartDecoder] parseRequestLine");
        if (lastCheck == -1)
        {
            lastCheck     = accumulation.getReadPosi();
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
                if (accumulation.remainRead() == headLength)
                {
                    reqHead.setHeadBuffer(accumulation);
                    accumulation = null;
                }
                else
                {
                    reqHead.setHeadBuffer(accumulation.slice(headLength));
                }
                parseBodyType();
                // 如果没有 body，设置 last = true
                if (state == ParseState.NO_BODY)
                {
                    reqHead.setLast(true);
                }
                // 调用可重写的处理方法
                return doProcessRequestHead(next, reqHead);
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
            String transferEncoding = reqHead.getHeaders().get("Transfer-Encoding");
            if ("chunked".equalsIgnoreCase(transferEncoding))
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
        if (accumulation == null || accumulation.remainRead() == 0)
        {
            return false;
        }
        long left   = reqHead.getContentLength() - bodyRead;
        int  remain = accumulation.remainRead();
        if (remain >= left)
        {
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
            part.setLast(true);
            if (remain > left)
            {
                part.setPart(accumulation.slice((int) left));
            }
            else
            {
                part.setPart(accumulation);
                accumulation = null;
            }
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
            next.fireRead(part);
            return false;
        }
    }

    private boolean parseBodyChunked(ReadProcessorNode next)
    {
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
                    break;
                }
            }
        }
        if (chunkSize == -1)
        {
            return false;
        }
        if (chunkSize == 0)
        {
            int totalLength = chunkSizeLineLength + 2;
            if (accumulation.remainRead() < totalLength)
            {
                return false;
            }
            HttpRequestChunkedBodyPart part = new HttpRequestChunkedBodyPart();
            part.setHeadLength(chunkSizeLineLength);
            part.setChunkLength(totalLength);
            part.setPart(accumulation.slice(totalLength));
            part.setLast(true);
            next.fireRead(part);
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        // 检查是否已接收完整的 chunk（chunk size 行 + chunk 数据 + 结尾 CRLF）
        int totalChunkLength = chunkSizeLineLength + chunkSize + 2;
        if (accumulation.remainRead() < totalChunkLength)
        {
            return false;
        }
        HttpRequestChunkedBodyPart part = new HttpRequestChunkedBodyPart();
        part.setHeadLength(chunkSizeLineLength);
        part.setChunkLength(totalChunkLength);
        part.setPart(accumulation.slice(totalChunkLength));
        part.setLast(false);
        next.fireRead(part);
        chunkSize           = -1;
        chunkSizeLineLength = -1;
        return true;
    }

    /**
     * 处理解析完成的请求头。
     * 子类可重写此方法来自定义请求头的处理逻辑（如 WebSocket 升级）。
     *
     * @param next 下一个处理节点
     * @param head 解析完成的请求头
     * @return true 表示需要继续处理（accumulation 中还有数据），false 表示处理完成
     */
    protected boolean doProcessRequestHead(ReadProcessorNode next, HttpRequestPartHead head)
    {
        next.fireRead(head);
        // 如果没有 body，直接重置状态
        if (state == ParseState.NO_BODY)
        {
            resetState();
        }
        if (accumulation == null)
        {
            return false;
        }
        else
        {
            accumulation.compact();
            return true;
        }
    }

    private void resetState()
    {
        reqHead             = null;
        headStartPosi       = -1;
        bodyRead            = 0;
        chunkSize           = -1;
        chunkSizeLineLength = -1; // 新增：重置chunk size行长度
        state               = ParseState.REQUEST_LINE;
        if (accumulation != null)
        {
            if (accumulation.remainRead() != 0)
            {
                accumulation.compact();
            }
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        if (reqHead != null)
        {
            reqHead.close();
            reqHead = null;
        }
        super.readFailed(e, next);
    }

    enum ParseState
    {
        REQUEST_LINE, REQUEST_HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
