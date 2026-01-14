package cc.jfire.jnet.extend.http.coder;

import cc.jfire.jnet.common.buffer.buffer.IoBuffer;
import cc.jfire.jnet.common.util.DataIgnore;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ClientSSLProtocol implements DataIgnore
{
    private boolean  startHandshake = false;
    private boolean  closeNotify    = false;
    private IoBuffer data;
}
