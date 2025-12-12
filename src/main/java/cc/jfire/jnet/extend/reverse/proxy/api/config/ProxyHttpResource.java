package cc.jfire.jnet.extend.reverse.proxy.api.config;

import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.FullMatchProxyHttpHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.PrefixMatchProxyHttpHandler;
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
    public ResourceHandler parse()
    {
        return switch (matchType)
        {
            case FULL -> new FullMatchProxyHttpHandler(match, proxy);
            case PREFIX -> new PrefixMatchProxyHttpHandler(match, proxy);
        };
    }
}
