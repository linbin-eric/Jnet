package com.jfirer.jnet.common.exception;

/**
 * 表明读取的数据不符合报文协议
 *
 * @author 林斌
 */
public class NotFitProtocolException extends JnetException
{

    public static final  NotFitProtocolException instance         = new NotFitProtocolException();
    /**
     *
     */
    private static final long                    serialVersionUID = -246003536673386746L;

    private NotFitProtocolException()
    {
    }
}
