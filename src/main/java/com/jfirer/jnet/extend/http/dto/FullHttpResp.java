package com.jfirer.jnet.extend.http.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FullHttpResp implements HttpRespPart
{
    private HttpRespHead head = new HttpRespHead();
    private HttpRespBody body = new HttpRespBody();
}
