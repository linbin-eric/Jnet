package com.jfirer.jnet.extend.http.client;

import com.jfirer.jnet.client.JnetClient;
import com.jfirer.jnet.common.recycler.RecycleHandler;
import lombok.Data;

import java.util.concurrent.SynchronousQueue;

@Data
public class ClientWrapper
{
    JnetClient                    client;
    SynchronousQueue<Object>      sync;
    long                          lastRespoonseTime;
    RecycleHandler<ClientWrapper> handler;

    public ClientWrapper(JnetClient client, SynchronousQueue<Object> sync)
    {
        this.client = client;
        this.sync = sync;
        lastRespoonseTime = System.currentTimeMillis();
    }
}
