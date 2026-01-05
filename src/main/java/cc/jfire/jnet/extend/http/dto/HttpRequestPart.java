package cc.jfire.jnet.extend.http.dto;

public interface HttpRequestPart
{
    void close();

    default boolean isLast()
    {
        return false;
    }
}
