package com.jfirer.jnet.extend.reverse.proxy.api.config;

import com.jfirer.jnet.extend.reverse.proxy.api.ResourceConfig;
import com.jfirer.jnet.extend.reverse.proxy.api.ResourceHandler;
import com.jfirer.jnet.extend.reverse.proxy.api.handler.ClassResourceHandler;
import com.jfirer.jnet.extend.reverse.proxy.api.handler.FileResourceHandler;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class IOResourceConfig implements ResourceConfig
{
    private final String prefixMatch;
    private final String path;
    private final int    order;

    public IOResourceConfig(String prefixMatch, String path, int order)
    {
        this.prefixMatch = prefixMatch;
        this.path        = path;
        this.order       = order;
    }

    @Override
    public ResourceHandler parse()
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
