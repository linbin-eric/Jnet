package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.coder.AbstractDecoder;
import cc.jfire.jnet.common.util.HttpDecodeUtil;
import cc.jfire.jnet.extend.http.dto.HttpRequestChunkedBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestFixLengthBodyPart;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartEnd;
import cc.jfire.jnet.extend.http.dto.HttpRequestPartHead;

import java.nio.charset.StandardCharsets;

public class HttpRequestPartDecoder extends AbstractDecoder
{
    private static final int                 MAX_CHUNK_SIZE_LINE_LENGTH = 32;
    private              int                 lastCheck                  = -1;
    private              ParseState          state                      = ParseState.REQUEST_LINE;
    private              HttpRequestPartHead reqHead;
    private              int                 headStartPosi              = -1;
    private              int                 bodyRead                   = 0;
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
                case NO_BODY ->
                {
                    next.fireRead(new HttpRequestPartEnd());
                    resetState();
                    yield accumulation != null && accumulation.remainRead() > 0;
                }
            };
        } while (goToNextState);
    }

    private boolean parseRequestLine()
    {
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
                reqHead.setUrl(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString());
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
                int headLength = headEndPosi - headStartPosi;
                accumulation.setReadPosi(headStartPosi);
                reqHead.setHeadBuffer(accumulation.slice(headLength));
                lastCheck = -1;
                HttpDecodeUtil.findAllHeaders(accumulation, reqHead::addHeader);
                HttpDecodeUtil.findContentLength(reqHead.getHeaders(), reqHead::setContentLength);
                parseBodyType();
                next.fireRead(reqHead);
                return true;
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
        if (accumulation == null || accumulation.remainRead() == 0)
        {
            return false;
        }
        int left   = reqHead.getContentLength() - bodyRead;
        int remain = accumulation.remainRead();
        if (remain > left)
        {
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
            part.setPart(accumulation.slice(left));
            next.fireRead(part);
            next.fireRead(new HttpRequestPartEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }
        else if (remain == left)
        {
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
            part.setPart(accumulation);
            accumulation = null;
            next.fireRead(part);
            next.fireRead(new HttpRequestPartEnd());
            resetState();
            return false;
        }
        else
        {
            bodyRead += remain;
            HttpRequestFixLengthBodyPart part = new HttpRequestFixLengthBodyPart();
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
                    chunkSize = Integer.parseInt(StandardCharsets.US_ASCII.decode(accumulation.readableByteBuffer(i)).toString().trim(), 16);
                    chunkSizeLineLength = i + 2 - startPosi; // 记录chunk size行的长度（包含CRLF）
                    // 注意：这里不移动readPosi，保持在chunk size行的起始位置
                    break;
                }
            }
        }
        if (chunkSize == -1)
        {
            return false;
        }
        // 处理chunk size为0的情况（结束标志）
        if (chunkSize == 0)
        {
            // chunk size为0时，格式为：0\r\n\r\n（chunk size行 + 结尾的CRLF）
            int totalLength = chunkSizeLineLength + 2;
            if (accumulation.remainRead() < totalLength)
            {
                return false;
            }
            // 跳过整个结束chunk
            accumulation.addReadPosi(totalLength);
            next.fireRead(new HttpRequestPartEnd());
            resetState();
            return accumulation != null && accumulation.remainRead() > 0;
        }

        // 检查是否已接收完整的chunk（chunk size行 + chunk数据 + 结尾CRLF）
        int totalChunkLength = chunkSizeLineLength + chunkSize + 2;
        if (accumulation.remainRead() < totalChunkLength)
        {
            return false;
        }

        // 创建HttpRequestChunkedBodyPart对象
        HttpRequestChunkedBodyPart part = new HttpRequestChunkedBodyPart();
        part.setHeadLength(chunkSizeLineLength);
        part.setChunkLength(totalChunkLength);
        // 从当前读取位置（chunk size行起始）截取完整的chunk内容
        part.setPart(accumulation.slice(totalChunkLength));

        next.fireRead(part);
        chunkSize = -1;
        chunkSizeLineLength = -1; // 重置chunk size行长度
        return true;
    }

    private void resetState()
    {
        reqHead       = null;
        headStartPosi = -1;
        bodyRead      = 0;
        chunkSize = -1;
        chunkSizeLineLength = -1; // 新增：重置chunk size行长度
        state     = ParseState.REQUEST_LINE;
        if (accumulation != null && accumulation.remainRead() == 0)
        {
            accumulation.free();
            accumulation = null;
        }
    }

    enum ParseState
    {
        REQUEST_LINE, REQUEST_HEADER, NO_BODY, BODY_FIX_LENGTH, BODY_CHUNKED
    }
}
