package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.ClientChannel;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Data;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.BlockingQueue;

@Data
public class ClientConnection
{
    ClientChannel                      client;
    BlockingQueue<HttpReceiveResponse> sync;
    long                               lastRespoonseTime;
    RecycleHandler<ClientConnection>   handler;
    public static final HttpReceiveResponse CLOSE_OF_CONNECTION = new HttpReceiveResponse();
    public static final long                KEEP_ALIVE_TIME     = 1000 * 60 * 5;

    public ClientConnection(ClientChannel client, BlockingQueue<HttpReceiveResponse> sync)
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
        HttpReceiveResponse response = sync.poll();
        if (response == CLOSE_OF_CONNECTION)
        {
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
