package cc.jfire.jnet.common.util;

public class MathUtil
{
    static final int smallestMask = ~15;

    /**
     * 计算大于CAP的最小的2的次方幂
     *
     * @param cap
     * @return
     */
    public static final int normalizeSize(int cap)
    {
        int n = cap - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }

    /**
     * 计算最接近value的log2值的整数。该整数小于等于log2值
     *
     * @param value
     * @return
     */
    public static int log2(int value)
    {
        return 31 - Integer.numberOfLeadingZeros(value);
    }
}
