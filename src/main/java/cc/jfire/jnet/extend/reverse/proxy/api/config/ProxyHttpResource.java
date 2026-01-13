package cc.jfire.jnet.extend.reverse.proxy.api.config;

import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.ProxyHttpHandler;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public record ProxyHttpResource(MatchType matchType, String match, String proxy, int order) implements ResourceConfig
{
    @Override
    public ResourceHandler parse(HttpConnectionPool pool)
    {
        return switch (matchType)
        {
            case FULL -> new ProxyHttpHandler(match, proxy, ProxyHttpHandler.MatchMode.EXACT);
            case PREFIX -> new ProxyHttpHandler(match, proxy, ProxyHttpHandler.MatchMode.PREFIX);
        };
    }

    public enum MatchType
    {
        PREFIX, FULL
    }
}
