package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Data;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Data
public class HttpConnection
{
    ClientChannel client;
    final BlockingQueue<HttpReceiveResponse> sync;
    long                           lastRespoonseTime;
    RecycleHandler<HttpConnection> handler;
    public static final HttpReceiveResponse CLOSE_OF_CONNECTION = new HttpReceiveResponse();
    public static final long                KEEP_ALIVE_TIME     = 1000 * 60 * 5;

    public HttpConnection(ClientChannel client, BlockingQueue<HttpReceiveResponse> sync)
    {
        this.client = client;
        this.sync = sync;
        lastRespoonseTime = System.currentTimeMillis();
    }

    public boolean isConnectionClosed()
    {
        return !client.alive() || sync.peek() == CLOSE_OF_CONNECTION || (System.currentTimeMillis() - lastRespoonseTime) > KEEP_ALIVE_TIME;
    }

    public HttpReceiveResponse waitForResponse() throws ClosedChannelException
    {
        HttpReceiveResponse response = null;
        try
        {
            response = sync.poll(20, TimeUnit.SECONDS);
        }
        catch (InterruptedException e)
        {
            client.close();
            throw new ClosedChannelException();
        }
        if (response == null || response == CLOSE_OF_CONNECTION)
        {
            if (response == null)
            {
                System.err.println("超时没有响应");
                client.close(new NullPointerException("没有response"));
            }
            else
            {
                System.err.println("连接被关闭");
                client.close();
            }
            throw new ClosedChannelException();
        }
        else
        {
            return response;
        }
    }

    public void recycle()
    {
        lastRespoonseTime = System.currentTimeMillis();
        handler.recycle(this);
    }
}
