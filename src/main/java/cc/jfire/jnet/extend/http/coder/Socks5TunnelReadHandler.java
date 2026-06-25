package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.api.Pipeline;
import cc.jfire.jnet.common.api.ReadProcessorNode;
import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.coder.AbstractDecoder;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * SOCKS5 隧道读处理器。
 * 在隧道建立前处理 SOCKS5 握手与 CONNECT 响应，建立后透传数据给后续处理器。
 */
public class Socks5TunnelReadHandler extends AbstractDecoder
{
    public static final String         KEY               = "socks5TunnelReadHandler";
    private final       String         domain;
    private final       int            port;
    private final       String         username;
    private final       String         password;
    private final       CountDownLatch tunnelLatch       = new CountDownLatch(1);
    private volatile    boolean        tunnelEstablished = false;
    private volatile    Throwable      tunnelError       = null;
    private             Stage          stage             = Stage.METHOD_SELECTION;

    private enum Stage
    {
        METHOD_SELECTION, USERPASS_AUTH, CONNECT_RESPONSE, ESTABLISHED
    }

    public Socks5TunnelReadHandler(String domain, int port, String username, String password)
    {
        this.domain   = domain;
        this.port     = port;
        this.username = username;
        this.password = password;
    }

    @Override
    public void pipelineComplete(Pipeline pipeline, ReadProcessorNode next)
    {
        try
        {
            sendMethodSelection(pipeline);
        }
        catch (Throwable e)
        {
            setTunnelError(e);
        }
    }

    @Override
    protected void process0(ReadProcessorNode next)
    {
        try
        {
            while (true)
            {
                switch (stage)
                {
                    case METHOD_SELECTION:
                        if (accumulation.remainRead() < 2)
                        {
                            return;
                        }
                        int ver    = unsignedGet();
                        int method = unsignedGet();
                        if (ver != 0x05)
                        {
                            throw new RuntimeException("SOCKS5 代理返回非法版本: " + ver);
                        }
                        if (method == 0xff)
                        {
                            throw new RuntimeException("SOCKS5 代理不支持客户端可用认证方式");
                        }
                        if (useUsernamePassword())
                        {
                            if (method != 0x02)
                            {
                                throw new RuntimeException("SOCKS5 代理未选择用户名密码认证，method=0x" + Integer.toHexString(method));
                            }
                            sendUsernamePassword(next.pipeline());
                            stage = Stage.USERPASS_AUTH;
                        }
                        else
                        {
                            if (method != 0x00)
                            {
                                throw new RuntimeException("SOCKS5 代理未选择无认证方式，method=0x" + Integer.toHexString(method));
                            }
                            sendConnect(next.pipeline());
                            stage = Stage.CONNECT_RESPONSE;
                        }
                        break;
                    case USERPASS_AUTH:
                        if (accumulation.remainRead() < 2)
                        {
                            return;
                        }
                        ver = unsignedGet();
                        int status = unsignedGet();
                        if (ver != 0x01)
                        {
                            throw new RuntimeException("SOCKS5 用户名密码认证返回非法版本: " + ver);
                        }
                        if (status != 0x00)
                        {
                            throw new RuntimeException("SOCKS5 用户名密码认证失败，status=0x" + Integer.toHexString(status));
                        }
                        sendConnect(next.pipeline());
                        stage = Stage.CONNECT_RESPONSE;
                        break;
                    case CONNECT_RESPONSE:
                        parseConnectResponse(next);
                        return;
                    case ESTABLISHED:
                        fireRemaining(next);
                        return;
                }
            }
        }
        catch (Throwable e)
        {
            fail(e);
        }
    }

    private void parseConnectResponse(ReadProcessorNode next)
    {
        if (accumulation.remainRead() < 4)
        {
            return;
        }
        int start = accumulation.getReadPosi();
        int atyp  = accumulation.get(start + 3) & 0xff;
        int addressLength;
        if (atyp == 0x01)
        {
            addressLength = 4;
        }
        else if (atyp == 0x04)
        {
            addressLength = 16;
        }
        else if (atyp == 0x03)
        {
            if (accumulation.remainRead() < 5)
            {
                return;
            }
            addressLength = 1 + (accumulation.get(start + 4) & 0xff);
        }
        else
        {
            throw new RuntimeException("SOCKS5 代理返回未知地址类型: 0x" + Integer.toHexString(atyp));
        }
        int totalLength = 4 + addressLength + 2;
        if (accumulation.remainRead() < totalLength)
        {
            return;
        }
        int ver = accumulation.get(start) & 0xff;
        int rep = accumulation.get(start + 1) & 0xff;
        int rsv = accumulation.get(start + 2) & 0xff;
        accumulation.setReadPosi(start + totalLength);
        if (ver != 0x05)
        {
            throw new RuntimeException("SOCKS5 CONNECT 响应版本非法: " + ver);
        }
        if (rsv != 0x00)
        {
            throw new RuntimeException("SOCKS5 CONNECT 响应 RSV 非 0: " + rsv);
        }
        if (rep != 0x00)
        {
            throw new RuntimeException("SOCKS5 CONNECT 被代理服务器拒绝，rep=0x" + Integer.toHexString(rep));
        }
        stage             = Stage.ESTABLISHED;
        tunnelEstablished = true;
        tunnelLatch.countDown();
        next.firePipelineComplete(next.pipeline());
        fireRemaining(next);
    }

    private void fireRemaining(ReadProcessorNode next)
    {
        if (accumulation != null && accumulation.remainRead() > 0)
        {
            IoBuffer rest = accumulation;
            accumulation = null;
            next.fireRead(rest);
        }
        else if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
    }

    private void sendMethodSelection(Pipeline pipeline)
    {
        byte[]   data   = useUsernamePassword() ? new byte[]{0x05, 0x01, 0x02} : new byte[]{0x05, 0x01, 0x00};
        IoBuffer buffer = pipeline.allocator().allocate(data.length);
        buffer.put(data);
        pipeline.directWrite(buffer);
    }

    private void sendUsernamePassword(Pipeline pipeline)
    {
        byte[] userBytes     = username.getBytes(StandardCharsets.UTF_8);
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);
        if (userBytes.length < 1 || userBytes.length > 255 || passwordBytes.length < 1 || passwordBytes.length > 255)
        {
            throw new IllegalArgumentException("SOCKS5 用户名和密码长度必须在 1 到 255 字节之间");
        }
        IoBuffer buffer = pipeline.allocator().allocate(3 + userBytes.length + passwordBytes.length);
        buffer.put((byte) 0x01);
        buffer.put((byte) userBytes.length);
        buffer.put(userBytes);
        buffer.put((byte) passwordBytes.length);
        buffer.put(passwordBytes);
        pipeline.directWrite(buffer);
    }

    private void sendConnect(Pipeline pipeline)
    {
        Address  address = encodeAddress(domain);
        int      length  = 4 + address.bytes.length + 2 + (address.atyp == 0x03 ? 1 : 0);
        IoBuffer buffer  = pipeline.allocator().allocate(length);
        buffer.put((byte) 0x05);
        buffer.put((byte) 0x01);
        buffer.put((byte) 0x00);
        buffer.put((byte) address.atyp);
        if (address.atyp == 0x03)
        {
            buffer.put((byte) address.bytes.length);
        }
        buffer.put(address.bytes);
        buffer.put((byte) ((port >>> 8) & 0xff));
        buffer.put((byte) (port & 0xff));
        pipeline.directWrite(buffer);
    }

    private boolean useUsernamePassword()
    {
        boolean hasUsername = username != null && !username.isEmpty();
        boolean hasPassword = password != null && !password.isEmpty();
        if (hasUsername != hasPassword)
        {
            throw new IllegalArgumentException("SOCKS5 用户名和密码必须同时配置");
        }
        return hasUsername;
    }

    private int unsignedGet()
    {
        return accumulation.get() & 0xff;
    }

    private void fail(Throwable e)
    {
        tunnelError = e;
        tunnelLatch.countDown();
        if (accumulation != null)
        {
            accumulation.free();
            accumulation = null;
        }
    }

    @Override
    public void readFailed(Throwable e, ReadProcessorNode next)
    {
        fail(e);
        next.fireReadFailed(e);
    }

    public boolean awaitTunnelEstablished(long timeout, TimeUnit unit) throws InterruptedException
    {
        return tunnelLatch.await(timeout, unit);
    }

    public boolean isTunnelEstablished()
    {
        return tunnelEstablished;
    }

    public Throwable getTunnelError()
    {
        return tunnelError;
    }

    public void setTunnelError(Throwable error)
    {
        tunnelError = error;
        tunnelLatch.countDown();
    }

    private static Address encodeAddress(String host)
    {
        if (isIpv4Literal(host))
        {
            String[] parts = host.split("\\.");
            byte[]   bytes = new byte[4];
            for (int i = 0; i < 4; i++)
            {
                bytes[i] = (byte) Integer.parseInt(parts[i]);
            }
            return new Address(0x01, bytes);
        }
        if (host.indexOf(':') != -1)
        {
            try
            {
                if (host.indexOf('%') != -1)
                {
                    throw new IllegalArgumentException("SOCKS5 IPv6 地址暂不支持 zone id: " + host);
                }
                byte[] bytes = InetAddress.getByName(host).getAddress();
                if (bytes.length == 16)
                {
                    return new Address(0x04, bytes);
                }
            }
            catch (Exception e)
            {
                throw new IllegalArgumentException("非法 IPv6 地址: " + host, e);
            }
        }
        byte[] domainBytes = host.getBytes(StandardCharsets.UTF_8);
        if (domainBytes.length < 1 || domainBytes.length > 255)
        {
            throw new IllegalArgumentException("SOCKS5 域名长度必须在 1 到 255 字节之间: " + host);
        }
        return new Address(0x03, domainBytes);
    }

    private static boolean isIpv4Literal(String host)
    {
        String[] parts = host.split("\\.", -1);
        if (parts.length != 4)
        {
            return false;
        }
        for (String part : parts)
        {
            if (part.isEmpty() || part.length() > 3)
            {
                return false;
            }
            for (int i = 0; i < part.length(); i++)
            {
                if (!Character.isDigit(part.charAt(i)))
                {
                    return false;
                }
            }
            int value = Integer.parseInt(part);
            if (value < 0 || value > 255)
            {
                return false;
            }
        }
        return true;
    }

    private record Address(int atyp, byte[] bytes)
    {
    }
}
