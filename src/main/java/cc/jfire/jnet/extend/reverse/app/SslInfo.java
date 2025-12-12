package cc.jfire.jnet.extend.reverse.app;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class SslInfo
{
    private boolean enable;
    private String  cert;
    private String  password;
}
