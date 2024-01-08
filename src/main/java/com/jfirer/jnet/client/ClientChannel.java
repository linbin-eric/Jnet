package com.jfirer.jnet.client;

import com.jfirer.jnet.common.api.ChannelContextInitializer;
import com.jfirer.jnet.common.util.ChannelConfig;

import java.nio.channels.ClosedChannelException;

public interface ClientChannel
{
    static ClientChannel newClient(ChannelConfig channelConfig, ChannelContextInitializer initializer)
    {
        return new ClientChannelImpl(channelConfig, initializer);
    }

    /**
     * 创建链接。重复调用无效，如果在该链接已经被关闭，调用该方法会抛出异常。
     *
     * @return
     */
    boolean connect();

    /**
     * 该客户端创建的链接是否还有有效。该方法需要在connect方法调用后再调用。
     * 注意：这个状态可能是滞后的，只有链接被明确关闭后才会返回false。即，在返回true的情况下，链接也有可能已经失效了。
     *
     * @return
     */
    boolean alive();

    void write(Object data) throws ClosedChannelException;

    void close();

    void close(Throwable e);

    enum ConnectedState
    {
        NOT_INIT, CONNECTED, DISCONNECTED
    }
}
