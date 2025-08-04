package com.jfirer.jnet.extend.http.dto;

import lombok.Data;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import java.nio.ByteBuffer;

@Data
public class SSLEngineWrapper
{
    private final SSLEngine sslEngine;

    public synchronized SSLEngineResult unwrap(ByteBuffer src, ByteBuffer dst) throws SSLException
    {
        return sslEngine.unwrap(src, dst);
    }

    public synchronized SSLEngineResult wrap(ByteBuffer src, ByteBuffer dst) throws SSLException
    {
        return sslEngine.wrap(src, dst);
    }

    public synchronized SSLEngineResult.HandshakeStatus getHandshakeStatus()
    {
        return sslEngine.getHandshakeStatus();
    }

    public synchronized void closeInbound() throws SSLException
    {
        sslEngine.closeInbound();
    }

    public synchronized void closeOutbound()
    {
        sslEngine.closeOutbound();
    }

    public synchronized SSLSession getSession()
    {
        return sslEngine.getSession();
    }

    public synchronized Runnable getDelegatedTask()
    {
        return sslEngine.getDelegatedTask();
    }
}
