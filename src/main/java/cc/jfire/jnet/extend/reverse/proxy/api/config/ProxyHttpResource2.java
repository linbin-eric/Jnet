package cc.jfire.jnet.extend.reverse.proxy.api.config;

import cc.jfire.jnet.extend.http.client.HttpConnection2Pool;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.ProxyHttpHandler2;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProxyHttpResource2 implements ResourceConfig
{
    private final String prefixMatch;
    private final String proxy;
    private final int    order;

    @Override
    public ResourceHandler parse(HttpConnection2Pool pool)
    {
        return new ProxyHttpHandler2(prefixMatch, proxy);
    }
}
