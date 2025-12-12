package cc.jfire.jnet.extend.reverse.proxy.api;

import cc.jfire.jnet.extend.reverse.proxy.api.config.IOResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.config.ProxyHttpResource;

public interface ResourceConfig
{
    ResourceHandler parse();

    /**
     * 配置的优先级，数字越小优先级越高
     */
    int getOrder();

    static ResourceConfig io(String prefixMatch, String path, int order)
    {
        return new IOResourceConfig(prefixMatch, path, order);
    }

    static ResourceConfig fullMatch(String match, String proxy, int order)
    {
        return new ProxyHttpResource(ProxyHttpResource.MatchType.FULL, match, proxy, order);
    }

    static ResourceConfig prefixMatch(String prefix, String proxy, int order)
    {
        return new ProxyHttpResource(ProxyHttpResource.MatchType.PREFIX, prefix, proxy, order);
    }
}
