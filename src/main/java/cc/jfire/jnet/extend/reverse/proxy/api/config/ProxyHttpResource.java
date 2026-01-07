package cc.jfire.jnet.extend.reverse.proxy.api.config;

import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.ProxyHttpHandler2;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ProxyHttpResource implements ResourceConfig
{
    private final MatchType matchType;
    private final String    match;
    private final String    proxy;
    private final int       order;

    public enum MatchType
    {
        PREFIX, FULL
    }

    @Override
    public ResourceHandler parse(HttpConnectionPool pool)
    {
        return switch (matchType)
        {
            case FULL -> new ProxyHttpHandler2(match, proxy, ProxyHttpHandler2.MatchMode.EXACT);
            case PREFIX -> new ProxyHttpHandler2(match, proxy, ProxyHttpHandler2.MatchMode.PREFIX);
        };
    }
}
