package cc.jfire.jnet.extend.reverse.proxy.api.config;

import cc.jfire.jnet.extend.http.client.HttpConnectionPool;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceConfig;
import cc.jfire.jnet.extend.reverse.proxy.api.ResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.ClassResourceHandler;
import cc.jfire.jnet.extend.reverse.proxy.api.handler.FileResourceHandler;
import lombok.experimental.Accessors;

@Accessors(chain = true)
public record IOResourceConfig(String prefixMatch, String path, int order) implements ResourceConfig
{
    @Override
    public ResourceHandler parse(HttpConnectionPool pool)
    {
        if (path.startsWith("classpath:"))
        {
            return new ClassResourceHandler(prefixMatch, path);
        }
        else if (path.startsWith("file:"))
        {
            return new FileResourceHandler(prefixMatch, path);
        }
        else
        {
            throw new IllegalArgumentException();
        }
    }
}
