package cc.jfire.jnet.extend.http.dto;

public interface HttpResponsePart
{
    /**
     * 释放该 part 持有的资源（如 IoBuffer）；无资源则为 no-op。
     */
    default void free() {}

    /**
     * 是否是响应流的最后一个部分
     */
    default boolean isLast()
    {
        return false;
    }
}
