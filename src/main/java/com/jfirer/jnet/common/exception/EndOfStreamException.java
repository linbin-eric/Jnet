package com.jfirer.jnet.common.exception;

public class EndOfStreamException extends JnetException
{
    public static final  EndOfStreamException instance         = new EndOfStreamException();
    /**
     *
     */
    private static final long                 serialVersionUID = 8037997970885790653L;

    private EndOfStreamException()
    {
    }
}
