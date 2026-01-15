package cc.jfire.jnet.extend.http.coder;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.net.ssl.SSLEngine;

@Data
@EqualsAndHashCode(callSuper = true)
public class SSLDecoder extends AbstractSSLDecoder
{
    public SSLDecoder(SSLEngine sslEngine)
    {
        super(sslEngine);
    }
}
