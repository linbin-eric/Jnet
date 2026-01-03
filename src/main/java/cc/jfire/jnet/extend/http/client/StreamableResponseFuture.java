package cc.jfire.jnet.extend.http.client;

import cc.jfire.jnet.extend.http.dto.HttpResponsePart;

import java.util.function.Consumer;

public class StreamableResponseFuture implements ResponseFuture
{
    private final Consumer<HttpResponsePart> httpResponsePartConsumer;
    private final Consumer<Throwable>        errorConsumer;

    public StreamableResponseFuture(Consumer<HttpResponsePart> httpResponsePartConsumer, Consumer<Throwable> errorConsumer)
    {
        this.httpResponsePartConsumer = httpResponsePartConsumer;
        this.errorConsumer = errorConsumer;
    }

    @Override
    public void onReceive(HttpResponsePart part)
    {
        if (httpResponsePartConsumer == null)
        {
            return;
        }
        try
        {
            httpResponsePartConsumer.accept(part);
        }
        catch (Throwable e)
        {
            part.free();
            throw e;
        }
    }

    @Override
    public void onFail(Throwable error)
    {
        if (errorConsumer == null)
        {
            return;
        }
        errorConsumer.accept(error);
    }
}

